=============
Release 0.231
=============

**Highlights**
==============

**Details**
===========

SPI Changes
___________
* Change ``ConnectorMetadata#commitPartition`` into async operation, and rename it to ``ConnectorMetadata#commitPartitionAsync``.

Raptor Changes
______________
* Fix an issue where DATE_TRUNC may produce incorrect results at certain timestamp in America/Sao_Paulo.
* Improve correctness check for RowType columns. Add specific validation checks for the individual fields when validating a row column.
* Add table_supports_delta_delete property in Raptor to allow deletion happening in background. DELETE queries in Raptor can now delete data logically but relying on compactors to delete physical data.
* Replace the ``SchemaTableName`` parameter in ``ConnectorMetadata#createView`` with a ``ConnectorTableMetadata``.
* Rename error code from ``RAPTOR_LOCAL_FILE_SYSTEM_ERROR`` to ``RAPTOR_FILE_SYSTEM_ERROR``.
* Remove unused FilterVoidLambda interface in ArrayFilterFunction.
* Change ``storage.data-directory` from path to URI. For existing deployment on local flash, a scheme header "file://" should be added to the original config value.

**Contributors**
================

A Brown, C Davis, E Fisher, G Harris
