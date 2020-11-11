package no.ssb.rawdata.converter.app.freg.schema;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.apache.avro.Schema;

@Value
@Builder
public class FregSchemaAdapter {

    private final String rawdataItemName;

    @NonNull
    private final String targetItemName;

    @NonNull
    private final Schema schema;

    @NonNull
    private final String rootXmlName;

}
