package no.ssb.rawdata.converter.app.freg;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;
import no.ssb.rawdata.converter.app.freg.schema.SchemaDescriptor;

import java.util.HashSet;
import java.util.Set;

@ConfigurationProperties("rawdata.converter.freg")
@Data
public class FregRawdataConverterConfig {

    /**
     * <p>Schemas of the expected data elements that the converted data is expected to be compliant with</p>
     */
    private Set<SchemaDescriptor> dataElements = new HashSet<>();

}