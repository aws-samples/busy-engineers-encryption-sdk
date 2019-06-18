"""Deployment helper tool for Busy Engineer's Guide to the AWS Encryption SDK workshop, Python track."""
import argparse
import logging
import mimetypes
import os
import uuid
from collections import namedtuple
from configparser import ConfigParser
from typing import Iterable, Optional, Text

import boto3
from botocore.client import BaseClient
from botocore.exceptions import ClientError

HERE = os.path.abspath(os.path.dirname(__file__))

DeployConfig = namedtuple(
    "DeployConfig",
    (
        "cloudformation",
        "s3",
        "apigateway",
        "region_name",
        "stack_name",
        "template_file",
        "static_resources_dir",
        "lambda_zip_file",
        "lambda_s3_key",
    ),
)

UNIQUE_TAG = "%UNIQUE%"
LAMBDA_TAG = "%LAMBDACONFIG%"
PARAMETERS_TAG = "%PARAMETERS%"
LAMBDA_BOOTSTRAP = os.path.join(HERE, "bootstrap-lambda.yaml")
LAMBDA_UPDATE = os.path.join(HERE, "update-lambda.yaml")
PARAMETERS_BOOTSTRAP = ""
PARAMETERS_UPDATE = os.path.join(HERE, "update-parameters.yaml")
WAITER_CONFIG = dict(Delay=10)
STATIC_ASSETS_PREFIX = "static-assets/"
_LOGGER = logging.getLogger("Encryption SDK Workshop Deployer")


def _collect_config(config_filename: Text, lambda_filename: Optional[Text]) -> DeployConfig:
    """Collect the config data from the specified file."""
    parser = ConfigParser()
    parser.read(config_filename)

    config_dir = os.path.dirname(os.path.abspath(config_filename))
    if lambda_filename is not None:
        lambda_filename = os.path.abspath(lambda_filename)
    return DeployConfig(
        cloudformation=boto3.client("cloudformation", region_name=parser["deploy"]["region"]),
        s3=boto3.client("s3", region_name=parser["deploy"]["region"]),
        apigateway=boto3.client("apigateway", region_name=parser["deploy"]["region"]),
        region_name=parser["deploy"]["region"],
        stack_name=parser["deploy"]["stack_name"],
        template_file=os.path.join(config_dir, parser["resources"]["template"]),
        static_resources_dir=os.path.join(config_dir, parser["resources"]["static_assets"]),
        lambda_zip_file=lambda_filename,
        lambda_s3_key=parser["deploy"]["lambda_s3_key"],
    )


def _lambda_body(stack_exists: bool) -> Iterable[Text]:
    """Load the Lambda definition body to inject into the CloudFormation template."""
    filename = LAMBDA_UPDATE if stack_exists else LAMBDA_BOOTSTRAP
    with open(filename, "r") as snippet:
        for line in snippet:
            yield line.rstrip()


def _template_parameters(stack_exists: bool) -> Iterable[Text]:
    """Load the template parameters definition to inject into the CloudFormation template."""
    if not stack_exists:
        yield PARAMETERS_BOOTSTRAP

    else:
        with open(PARAMETERS_UPDATE, "r") as params:
            for line in params:
                yield line.rstrip()


def _template_lines(config: DeployConfig, stack_exists: bool) -> Iterable[Text]:
    """Load the template and perform all necessary injections."""
    deployment_id = uuid.uuid4().hex
    lambda_body = _lambda_body(stack_exists)
    parameters = _template_parameters(stack_exists)

    with open(config.template_file, "r") as template:
        for line in template:
            _line = line.rstrip().replace(UNIQUE_TAG, deployment_id)

            if _line.strip() == PARAMETERS_TAG:
                for param_line in parameters:
                    yield param_line

            elif _line.strip() == LAMBDA_TAG:
                indent = _line[: _line.index(LAMBDA_TAG)]
                for _lambda_line in lambda_body:
                    yield indent + _lambda_line
            else:
                yield _line


def _template_body(config: DeployConfig, stack_exists: bool) -> Text:
    """Load the modified template body."""
    return "\n".join(_template_lines(config, stack_exists))


def _stack_exists(config: DeployConfig) -> bool:
    """Determine if the stack has already been deployed."""
    try:
        config.cloudformation.describe_stacks(StackName=config.stack_name)

    except ClientError as error:
        if error.response["Error"]["Message"] == "Stack with id {name} does not exist".format(name=config.stack_name):
            return False
        raise

    else:
        return True


_BUCKET_NAME = None


def _bucket_name(config: DeployConfig) -> Text:
    """Find the bucket name from the CloudFormation stack.
    Cache this value; we need it rather frequently.
    """
    global _BUCKET_NAME
    if _BUCKET_NAME is not None:
        return _BUCKET_NAME

    response = config.cloudformation.describe_stack_resource(StackName=config.stack_name, LogicalResourceId="S3Bucket")
    _BUCKET_NAME = response["StackResourceDetail"]["PhysicalResourceId"]
    return _BUCKET_NAME


def _api_endpoint(config: DeployConfig) -> Text:
    """Find the API endpoint from the CloudFormation stack."""
    response = config.cloudformation.describe_stack_resource(StackName=config.stack_name, LogicalResourceId="RestAPI")
    rest_api_id = response["StackResourceDetail"]["PhysicalResourceId"]

    return "https://{api_id}.execute-api.{region}.amazonaws.com/test/".format(
        api_id=rest_api_id, region=config.region_name
    )


def _purge_bucket_keyspace(config: DeployConfig, prefix: Text) -> None:
    """Delete all S3 objects with a given key prefix."""
    bucket = _bucket_name(config)
    _LOGGER.info("Clearing bucket prefix s3://{bucket}/{prefix}".format(bucket=bucket, prefix=prefix))
    paginator = config.s3.get_paginator("list_object_versions")
    response_iterator = paginator.paginate(Bucket=bucket, Prefix=prefix)
    for page in response_iterator:
        keys = [{"Key": key["Key"], "VersionId": key["VersionId"]} for key in page.get("Versions", [])]
        if keys:
            _LOGGER.info("Deleting {} objects".format(len(keys)))
            config.s3.delete_objects(Bucket=bucket, Delete={"Objects": keys})
    _LOGGER.info("Bucket prefix clear")


def _upload_public_file_to_s3(s3: BaseClient, bucket: Text, filepath: Text, key: Text) -> None:
    """Upload a file to S3, setting the MIME type and making it publicly readable."""
    _LOGGER.info("Uploading {}".format(key))
    with open(filepath, "rb") as body:
        put_kwargs = dict(Bucket=bucket, Body=body, Key=key)
        mime_type, _encoding = mimetypes.guess_type(key)
        if mime_type is not None:
            put_kwargs["ContentType"] = mime_type
        s3.put_object(**put_kwargs)
        s3.put_object_acl(Bucket=bucket, Key=key, ACL="public-read")


def _upload_directory_to_s3(config: DeployConfig, prefix: Text) -> None:
    """Upload an entire directory tree into the S3 bucket, assigning the specified prefix to all files."""
    _LOGGER.info("Uploading files")
    bucket = _bucket_name(config)
    for root, dirs, files in os.walk(config.static_resources_dir):
        for _file in files:
            path = os.path.join(root, _file)
            _key = STATIC_ASSETS_PREFIX + path[len(config.static_resources_dir) + 1 :]
            _upload_public_file_to_s3(config.s3, bucket, path, _key)
    _LOGGER.info("All files uploaded")


def _sync_static_assets_to_s3(config: DeployConfig) -> None:
    """Synchronize the local static assets directory with the keyspace in S3."""
    _purge_bucket_keyspace(config, prefix=STATIC_ASSETS_PREFIX)
    _upload_directory_to_s3(config, prefix=STATIC_ASSETS_PREFIX)


def _update_existing_stack(config: DeployConfig) -> None:
    """Update a stack."""
    _LOGGER.info("Updating existing stack")

    # 1. upload lambda zip
    _LOGGER.info("Uploading Lambda zip file")
    with open(config.lambda_zip_file, "rb") as lambda_file:
        response = config.s3.put_object(Bucket=_bucket_name(config), Body=lambda_file, Key=config.lambda_s3_key)

    # 2. collect template body
    template = _template_body(config, stack_exists=True)

    # 3. update stack
    config.cloudformation.update_stack(
        StackName=config.stack_name,
        TemplateBody=template,
        Parameters=[
            dict(ParameterKey="CodeKey", ParameterValue=config.lambda_s3_key),
            dict(ParameterKey="CodeVersion", ParameterValue=response["VersionId"]),
        ],
        Capabilities=["CAPABILITY_IAM"],
    )
    _LOGGER.info("Waiting for stack update to complete...")
    waiter = config.cloudformation.get_waiter("stack_update_complete")
    waiter.wait(StackName=config.stack_name, WaiterConfig=WAITER_CONFIG)
    _LOGGER.info("Stack update complete!")

    # 4. Synchronize static assets
    _sync_static_assets_to_s3(config)

    # 5. Collect the API endpoint
    endpoint = _api_endpoint(config)
    _LOGGER.info("Endpoint available at: {}".format(endpoint))


def _deploy_new_stack(config: DeployConfig) -> None:
    """Deploy a new stack."""
    _LOGGER.info("Bootstrapping new stack")

    # 1. collect bootstrapping template body
    template = _template_body(config, stack_exists=False)

    # 2. deploy template
    config.cloudformation.create_stack(
        StackName=config.stack_name, TemplateBody=template, Capabilities=["CAPABILITY_IAM"]
    )
    _LOGGER.info("Waiting for stack to deploy...")
    waiter = config.cloudformation.get_waiter("stack_create_complete")
    waiter.wait(StackName=config.stack_name, WaiterConfig=WAITER_CONFIG)
    _LOGGER.info("Stack deployment complete!")

    # 3. update stack with actual Lambda resource
    _update_existing_stack(config)


def _deploy_or_update(config: DeployConfig) -> None:
    """Update a stack, deploying a new stack if nothing exists yet."""
    if _stack_exists(config):
        return _update_existing_stack(config)

    return _deploy_new_stack(config)


def _destroy(config: DeployConfig) -> None:
    """Destroy the existing stack."""
    _LOGGER.info("Destroying stack")
    _purge_bucket_keyspace(config, prefix="")
    config.cloudformation.delete_stack(StackName=config.stack_name)
    _LOGGER.info("Waiting for stack to delete...")
    waiter = config.cloudformation.get_waiter("stack_delete_complete")
    waiter.wait(StackName=config.stack_name, WaiterConfig=WAITER_CONFIG)
    _LOGGER.info("Stack destroyed!")


_ACTIONS = {"deploy": _deploy_or_update, "destroy": _destroy}
_ZIP_REQUIRED = ("deploy",)


def _setup_logging() -> None:
    """Set up logging."""
    logging.basicConfig(level=logging.INFO)


def main(args=None) -> None:
    """Entry point for CLI."""
    _setup_logging()

    parser = argparse.ArgumentParser(description="Deployment helper")
    parser.add_argument("action", choices=_ACTIONS.keys(), help="What action should be taken?")
    parser.add_argument("--config", required=True, help="What config file should be used?")
    parser.add_argument("--lambda-zip", required=False, help="File containing built Lambda resource.")

    parsed = parser.parse_args(args)
    if parsed.action in _ZIP_REQUIRED and parsed.lambda_zip is None:
        parsed.error('"--lambda-zip" must be provided for "{action}" action'.format(action=parsed.action))

    config = _collect_config(parsed.config, parsed.lambda_zip)
    _ACTIONS[parsed.action](config)


if __name__ == "__main__":
    main()
