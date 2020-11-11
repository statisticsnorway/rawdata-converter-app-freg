package no.ssb.rawdata.converter.app.freg;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

@ConfigurationProperties("rawdata.converter.app.freg")
@Data
public class FregRawdataConverterConfig {

    /**
     * <p>Some config param</p>
     *
     * <p>Defaults to "default value"</p>
     *
     * TODO: Remove this
     */
    private String someParam = "default value";

}