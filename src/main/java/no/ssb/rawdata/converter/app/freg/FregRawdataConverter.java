package no.ssb.rawdata.converter.app.freg;

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import no.ssb.avro.convert.xml.XmlToRecords;
import no.ssb.rawdata.api.RawdataMessage;
import no.ssb.rawdata.converter.app.freg.schema.FregSchemaAdapter;
import no.ssb.rawdata.converter.app.freg.schema.FregSchemas;
import no.ssb.rawdata.converter.core.convert.ConversionResult;
import no.ssb.rawdata.converter.core.convert.ConversionResult.ConversionResultBuilder;
import no.ssb.rawdata.converter.core.convert.RawdataConverter;
import no.ssb.rawdata.converter.core.convert.ValueInterceptorChain;
import no.ssb.rawdata.converter.core.exception.RawdataConverterException;
import no.ssb.rawdata.converter.core.schema.AggregateSchemaBuilder;
import no.ssb.rawdata.converter.core.schema.DcManifestSchemaAdapter;
import no.ssb.rawdata.converter.util.RawdataMessageAdapter;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecordBuilder;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static no.ssb.rawdata.converter.util.RawdataMessageAdapter.posAndIdOf;

@Slf4j
public class FregRawdataConverter implements RawdataConverter {

    private static final String FIELDNAME_DC_MANIFEST = "dcManifest";

    private final FregRawdataConverterConfig converterConfig;
    private final ValueInterceptorChain valueInterceptorChain;

    private DcManifestSchemaAdapter dcManifestSchemaAdapter;
    private Schema targetAvroSchema;
    private final Set<FregSchemaAdapter> fregSchemas;
    private final Set<String> requiredRawdataItems;

    public FregRawdataConverter(FregRawdataConverterConfig converterConfig, ValueInterceptorChain valueInterceptorChain) {
        this.converterConfig = converterConfig;
        this.valueInterceptorChain = valueInterceptorChain;
        this.fregSchemas = converterConfig.getDataElements()
          .stream().map(schemaSource -> FregSchemas.getBySchemaSource(schemaSource))
          .collect(Collectors.toSet());
        this.requiredRawdataItems = fregSchemas.stream()
          .filter(schema -> !schema.getOptional())
          .map(schema -> schema.getRawdataItemName())
          .collect(Collectors.toSet());
    }

    @Override
    public void init(Collection<RawdataMessage> sampleRawdataMessages) {
        log.info("Determine target avro schema from {}", sampleRawdataMessages);
        RawdataMessage sample = sampleRawdataMessages.stream()
          .findFirst()
          .orElseThrow(() ->
            new FregRawdataConverterException("Unable to determine target avro schema since no sample rawdata messages were supplied. Make sure to configure `converter-settings.rawdata-samples`")
          );

        RawdataMessageAdapter msg = new RawdataMessageAdapter(sample);
        dcManifestSchemaAdapter = DcManifestSchemaAdapter.of(sample);

        String targetNamespace = "dapla.rawdata." + msg.getTopic().orElse("freg");
        AggregateSchemaBuilder targetSchemabuilder = new AggregateSchemaBuilder(targetNamespace)
          .schema(FIELDNAME_DC_MANIFEST, dcManifestSchemaAdapter.getDcManifestSchema());

        fregSchemas.forEach(schema -> {
            targetSchemabuilder.schema(schema.getTargetItemName(), schema.getSchema());
        });

        targetAvroSchema = targetSchemabuilder.build();
    }

    public DcManifestSchemaAdapter dcManifestSchemaAdapter() {
        if (dcManifestSchemaAdapter == null) {
            throw new IllegalStateException("dcManifestSchemaAdapter is null. Make sure RawdataConverter#init() was invoked in advance.");
        }

        return dcManifestSchemaAdapter;
    }

    @Override
    public Schema targetAvroSchema() {
        if (targetAvroSchema == null) {
            throw new IllegalStateException("targetAvroSchema is null. Make sure RawdataConverter#init() was invoked in advance.");
        }

        return targetAvroSchema;
    }

    @Override
    public boolean isConvertible(RawdataMessage rawdataMessage) {
        if (rawdataMessage.keys().containsAll(requiredRawdataItems)) {
            return true;
        }
        else {
            log.warn("Missing required rawdata items {}. Skipping rawdataMessage {}",
              Sets.difference(requiredRawdataItems, rawdataMessage.keys()),
              posAndIdOf(rawdataMessage));
            return false;
        }
    }

    @Override
    public ConversionResult convert(RawdataMessage rawdataMessage) {
        ConversionResultBuilder resultBuilder = ConversionResult.builder(new GenericRecordBuilder(targetAvroSchema));
        addDcManifest(rawdataMessage, resultBuilder);

        fregSchemas.forEach(schema -> {
            if (rawdataMessage.keys().contains(schema.getRawdataItemName())) {
                convertXml(rawdataMessage, resultBuilder, schema);
            }
        });

        return resultBuilder.build();
    }

    void addDcManifest(RawdataMessage rawdataMessage, ConversionResultBuilder resultBuilder) {
        resultBuilder.withRecord(FIELDNAME_DC_MANIFEST, dcManifestSchemaAdapter().newRecord(rawdataMessage));
    }

    void convertXml(RawdataMessage rawdataMessage, ConversionResultBuilder resultBuilder, FregSchemaAdapter schemaAdapter) {
        byte[] data = rawdataMessage.get(schemaAdapter.getRawdataItemName());
        try (XmlToRecords records = new XmlToRecords(new ByteArrayInputStream(data), schemaAdapter.getRootElementName(), schemaAdapter.getSchema(), valueInterceptorChain::intercept)) {
            records.forEach(record ->
              resultBuilder.withRecord(schemaAdapter.getTargetItemName(), record)
            );
        }
        catch (Exception e) {
            throw new FregRawdataConverterException("Error converting freg " + schemaAdapter.getRawdataItemName() + " data at " + posAndIdOf(rawdataMessage), e);
        }
    }

    public static class FregRawdataConverterException extends RawdataConverterException {
        public FregRawdataConverterException(String msg) {
            super(msg);
        }
        public FregRawdataConverterException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}