"""Sphinx configuration."""
from datetime import datetime

project = "A Busy Engineer's Guide to the AWS Encryption SDK"

source_suffix = ".rst"  # The suffix of source filenames.
master_doc = "index"  # The master toctree document.

copyright = u"Copyright 2017-%s Amazon.com, Inc. or its affiliates." % datetime.now().year # pylint: disable=redefined-builtin

# List of directories, relative to source directory, that shouldn't be searched
# for source files.
exclude_trees = ["_build"]

extensions = ["sphinx_tabs.tabs", "sphinx_copybutton"]

pygments_style = "sphinx"

autoclass_content = "both"
autodoc_default_flags = ["show-inheritance", "members"]
autodoc_member_order = "bysource"

html_theme = "alabaster"
html_static_path = ["_static"]
htmlhelp_basename = "%sdoc" % project

# autosummary
autosummary_generate = True

rst_prolog = """
.. |region| replace:: us-east-2
"""
