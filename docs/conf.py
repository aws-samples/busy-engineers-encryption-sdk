"""Sphinx configuration."""
from datetime import datetime

project = "re:Invent SID345 Workshop"

source_suffix = ".rst"  # The suffix of source filenames.
master_doc = "index"  # The master toctree document.

copyright = u"%s, Amazon" % datetime.now().year  # pylint: disable=redefined-builtin

# List of directories, relative to source directory, that shouldn't be searched
# for source files.
exclude_trees = ["_build"]

pygments_style = "sphinx"

autoclass_content = "both"
autodoc_default_flags = ["show-inheritance", "members"]
autodoc_member_order = "bysource"

html_theme = "sphinx_rtd_theme"
html_static_path = ["_static"]
htmlhelp_basename = "%sdoc" % project

# Example configuration for intersphinx: refer to the Python standard library.
intersphinx_mapping = {"python": ("http://docs.python.org/", None)}

# autosummary
autosummary_generate = True
