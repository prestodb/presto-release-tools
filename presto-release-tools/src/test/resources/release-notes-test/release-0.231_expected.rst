=============
Release 0.231
=============

**Highlights**
==============

**Details**
===========

SPI Changes
___________
* Change ``ConnectorMetadata#commitPartition`` into async operation, and rename it to ``ConnectorMetadata#commitPartitionAsync``. (`#2 <https://github.com/prestodb/presto/pull/2>`_)

Raptor Changes
______________
* Fix an issue where DATE_TRUNC may produce incorrect results at certain timestamp in America/Sao_Paulo. (`#1 <https://github.com/prestodb/presto/pull/1>`_)
* Improve correctness check for RowType columns. Add specific validation checks for the individual fields when validating a row column. (`#2 <https://github.com/prestodb/presto/pull/2>`_)
* Add table_supports_delta_delete property in Raptor to allow deletion happening in background. DELETE queries in Raptor can now delete data logically but relying on compactors to delete physical data. (`#2 <https://github.com/prestodb/presto/pull/2>`_)
* Replace the ``SchemaTableName`` parameter in ``ConnectorMetadata#createView`` with a ``ConnectorTableMetadata``. (`#1 <https://github.com/prestodb/presto/pull/1>`_)
* Rename error code from ``RAPTOR_LOCAL_FILE_SYSTEM_ERROR`` to ``RAPTOR_FILE_SYSTEM_ERROR``. (`#1 <https://github.com/prestodb/presto/pull/1>`_)
* Remove unused FilterVoidLambda interface in ArrayFilterFunction. (`#2 <https://github.com/prestodb/presto/pull/2>`_)
* Change ``storage.data-directory` from path to URI. For existing deployment on local flash, a scheme header "file://" should be added to the original config value. (`#1 <https://github.com/prestodb/presto/pull/1>`_)

**Credits**
===========

A Brown, C Davis, E Fisher, G Harris
