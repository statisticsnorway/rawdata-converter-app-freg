package no.ssb.rawdata.converter.app.freg;

import no.ssb.rawdata.converter.app.freg.schema.SchemaDescriptor;
import no.ssb.rawdata.converter.core.convert.ConversionResult;
import no.ssb.rawdata.converter.core.convert.ValueInterceptorChain;
import no.ssb.rawdata.converter.test.message.RawdataMessageFixtures;
import no.ssb.rawdata.converter.test.message.RawdataMessages;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Set;

@Disabled
public class FregRawdataConverterTest {

    static RawdataMessageFixtures fixtures;

    @BeforeAll
    static void loadFixtures() {
        fixtures = RawdataMessageFixtures.init("private");
    }

    @Disabled
    @Test
    void shouldConvertRawdataMessages() {
        RawdataMessages messages = fixtures.rawdataMessages("private");
        FregRawdataConverterConfig config = new FregRawdataConverterConfig();
        config.setDataElements(Set.of(
          new SchemaDescriptor("freg-hendelse-v1_4_2"),
          new SchemaDescriptor("freg-person-v1_4")
        ));

        FregRawdataConverter converter = new FregRawdataConverter(config, new ValueInterceptorChain());
        converter.init(messages.index().values());
        ConversionResult res = converter.convert(messages.index().get("13174063"));
    }

}
