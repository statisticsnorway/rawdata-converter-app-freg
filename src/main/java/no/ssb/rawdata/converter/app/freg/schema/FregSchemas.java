package no.ssb.rawdata.converter.app.freg.schema;

import com.google.common.base.Functions;
import no.ssb.rawdata.converter.app.freg.FregRawdataConverter.FregRawdataConverterException;
import no.ssb.rawdata.converter.util.WordUtil;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static no.ssb.rawdata.converter.util.AvroSchemaUtil.readAvroSchema;

public class FregSchemas {

    private static final Map<String, FregSchemaAdapter> SCHEMAS_BY_RAWDATA_ITEM_NAME;

    static {
        Set<FregSchemaAdapter> SCHEMAS = Set.of(
          FregSchemaAdapter.builder()
            .rawdataItemName("event")
            .targetItemName("hendelse")
            .schema(readAvroSchema("schema/freg-hendelse_v1.4.avsc"))
            .rootXmlName("dokumentForHendelse")
            .build(),
          FregSchemaAdapter.builder()
            .rawdataItemName("person")
            .targetItemName("person")
            .schema(readAvroSchema("schema/freg-person_v1.4.avsc"))
            .rootXmlName("folkeregisterperson")
            .build()
        );

        SCHEMAS_BY_RAWDATA_ITEM_NAME = SCHEMAS.stream()
          .collect(Collectors.toMap(
            s -> s.getRawdataItemName(),
            Functions.identity())
          );
    }

    public static FregSchemaAdapter getByRawdataItemName(String rawdataItemName) {
        return Optional.ofNullable(SCHEMAS_BY_RAWDATA_ITEM_NAME.get(rawdataItemName))
          .orElseThrow(() ->
            new SchemaNotFoundException("No freg schema found for rawdata item: " + rawdataItemName));
    }

    public static class SchemaNotFoundException extends FregRawdataConverterException {
        public SchemaNotFoundException(String msg) {
            super(msg);
        }
    }

}
