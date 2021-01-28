package no.ssb.rawdata.converter.app.freg.schema;

import no.ssb.rawdata.converter.app.freg.FregRawdataConverter.FregRawdataConverterException;

import java.util.Set;

import static no.ssb.rawdata.converter.util.AvroSchemaUtil.readAvroSchema;

public class FregSchemas {

    private static final Set<FregSchemaAdapter> SCHEMAS;

    static {
        SCHEMAS = Set.of(
          FregSchemaAdapter.builder()
            .schemaName("freg-hendelse-v1_4")
            .schema(readAvroSchema("schema/freg-hendelse-v1_4.avsc"))
            .rawdataItemName("event")
            .targetItemName("hendelse")
            .rootElementName("dokumentForHendelse")
            .build(),
          FregSchemaAdapter.builder()
            .schemaName("freg-hendelse-v1_4_2")
            .schema(readAvroSchema("schema/freg-hendelse-v1_4_2.avsc"))
            .rawdataItemName("event")
            .targetItemName("hendelse")
            .rootElementName("dokumentForHendelse")
            .build(),
          FregSchemaAdapter.builder()
            .schemaName("freg-hendelse-v1_4_3")
            .schema(readAvroSchema("schema/freg-hendelse-v1_4_3.avsc"))
            .rawdataItemName("event")
            .targetItemName("hendelse")
            .rootElementName("dokumentForHendelse")
            .build(),
          FregSchemaAdapter.builder()
            .schemaName("freg-person-v1_4")
            .schema(readAvroSchema("schema/freg-person-v1_4.avsc"))
            .rawdataItemName("person")
            .targetItemName("person")
            .rootElementName("folkeregisterperson")
            .build(),
          FregSchemaAdapter.builder()
            .schemaName("freg-person-v1_4_3")
            .schema(readAvroSchema("schema/freg-person-v1_4_3.avsc"))
            .rawdataItemName("person")
            .targetItemName("person")
            .rootElementName("folkeregisterperson")
            .build(),
          FregSchemaAdapter.builder()
            .schemaName("brsv-hendelsefeed-v1_0")
            .schema(readAvroSchema("schema/brsv-hendelsefeed-v1_0.avsc"))
            .rawdataItemName("event")
            .targetItemName("hendelse")
            .rootElementName("dokumentForHendelse")
            .build(),
          FregSchemaAdapter.builder()
            .schemaName("brsv-person-v1_0")
            .schema(readAvroSchema("schema/brsv-person-v1_0.avsc"))
            .rawdataItemName("person")
            .targetItemName("person")
            .rootElementName("folkeregisterperson")
            .build()
        );
    }

    public static FregSchemaAdapter getBySchemaDescriptor(SchemaDescriptor schemaSource) {
        FregSchemaAdapter fregSchemaAdapter = SCHEMAS.stream()
          .filter(schema -> schema.getSchemaName().equalsIgnoreCase(schemaSource.getSchemaName()))
          .findFirst()
          .orElseThrow(() ->
            new SchemaNotFoundException("No schema found for " + schemaSource.getSchemaName()));
        fregSchemaAdapter = merge(fregSchemaAdapter, schemaSource);

        return fregSchemaAdapter;
    }

    private static FregSchemaAdapter merge(FregSchemaAdapter fregSchemaAdapter, SchemaDescriptor overrides) {
        FregSchemaAdapter.FregSchemaAdapterBuilder builder = fregSchemaAdapter.toBuilder();
        if (overrides.getRawdataItemName() != null) {
            builder.rawdataItemName(overrides.getRawdataItemName());
        }
        if (overrides.getTargetItemName() != null) {
            builder.targetItemName(overrides.getTargetItemName());
        }
        if (overrides.getOptional() != null) {
            builder.optional(overrides.getOptional());
        }
        if (overrides.getRootElementName() != null) {
            builder.rootElementName(overrides.getRootElementName());
        }

        return builder.build();
    }

    public static class SchemaNotFoundException extends FregRawdataConverterException {
        public SchemaNotFoundException(String msg) {
            super(msg);
        }
    }

}
