"""Tooling to listen for calls to KMS by watching botocore's logs."""
import io
import logging
from typing import Callable

_KMS_DATA_PLANE_OPERATIONS = (
    "TrentService.Encrypt",
    "TrentService.Decrypt",
    "TrentService.GenerateDataKey",
    "TrentService.GenerateDataKeyWithoutPlaintext",
    "TrentService.ReEncrypt",
    "TrentService.GenerateRandom",
)


def kms_data_plane_request_filter(log_entry: str) -> bool:
    """Determine whether or not this log entry is a KMS data plane request."""
    if "Making request" not in log_entry:
        return False

    return any([operation in log_entry for operation in _KMS_DATA_PLANE_OPERATIONS])


class CountingListeningStringIO(io.StringIO):
    """Stream listener that counts the number of writes that match a filter.

    :param callable filter_function: Filter that will be called on each write. Must return a bool.
    """

    def __init__(self, filter_function: Callable) -> None:
        """Set up unique resources."""
        self.matching_calls = 0
        self._filter_function = filter_function
        super(CountingListeningStringIO, self).__init__()

    def write(self, s: str) -> None:
        """Iterate the matching calls count if the line matches the filter.

        .. note::

            Nothing written to this stream is persisted.

        :param str s: Data to evaluate
        """
        if self._filter_function(s):
            self.matching_calls += 1


class KmsLogListener:
    """Listen for logs entries that indicate calls made from botocore for KMS data plane operations."""

    def __init__(self):
        """Set up log handlers."""
        formatter = logging.Formatter("%(message)s")
        self._counter = CountingListeningStringIO(filter_function=kms_data_plane_request_filter)
        self._handler = logging.StreamHandler(stream=self._counter)
        self._handler.setFormatter(formatter)
        self._handler.setLevel(logging.DEBUG)

        self._logger = logging.getLogger("botocore.endpoint")
        self._logger.addHandler(self._handler)

    @property
    def matching_entries(self) -> int:
        """Get matching log entries found since last reset."""
        return self._counter.matching_calls

    def reset_matching_entries(self) -> None:
        """Reset matching log entries count."""
        self._counter.matching_calls = 0
