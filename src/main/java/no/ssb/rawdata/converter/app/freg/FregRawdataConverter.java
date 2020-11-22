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
import no.ssb.rawdata.converter.util.AvroSchemaUtil;
import no.ssb.rawdata.converter.util.RawdataMessageAdapter;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static no.ssb.rawdata.converter.util.RawdataMessageAdapter.posAndIdOf;

@Slf4j
public class FregRawdataConverter implements RawdataConverter {

    private static final String FIELDNAME_MANIFEST = "manifest";
    private static final String FIELDNAME_DC_MANIFEST = "collector";
    private static final String FIELDNAME_CONVERTER_MANIFEST = "converter";

    private final FregRawdataConverterConfig converterConfig;
    private final ValueInterceptorChain valueInterceptorChain;

    private final Set<FregSchemaAdapter> fregSchemas;
    private final Set<String> requiredRawdataItems;
    private final Schema converterManifestSchema;
    private DcManifestSchemaAdapter dcManifestSchemaAdapter;
    private Schema manifestSchema;
    private Schema targetAvroSchema;

    public FregRawdataConverter(FregRawdataConverterConfig converterConfig, ValueInterceptorChain valueInterceptorChain) {
        this.converterConfig = converterConfig;
        this.valueInterceptorChain = valueInterceptorChain;
        this.converterManifestSchema = AvroSchemaUtil.readAvroSchema("schema/converter-manifest.avsc");
        this.fregSchemas = converterConfig.getDataElements()
          .stream().map(schemaDescriptor -> FregSchemas.getBySchemaDescriptor(schemaDescriptor))
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

        String targetNamespace = "dapla.rawdata.ske.freg" + msg.getTopic().orElse("dataset");

        manifestSchema = new AggregateSchemaBuilder("dapla.rawdata.manifest")
          .schema("collector", dcManifestSchemaAdapter.getDcManifestSchema())
          .schema("converter", converterManifestSchema)
          .build();

        AggregateSchemaBuilder targetSchemaBuilder = new AggregateSchemaBuilder(targetNamespace)
          .schema("manifest", manifestSchema);

        fregSchemas.forEach(schema -> {
            targetSchemaBuilder.schema(schema.getTargetItemName(), schema.getSchema());
        });

        targetAvroSchema = targetSchemaBuilder.build();
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
        ConversionResultBuilder resultBuilder = ConversionResult.builder(targetAvroSchema, rawdataMessage);

        addManifest(rawdataMessage, resultBuilder);
        fregSchemas.forEach(schema -> {
            if (rawdataMessage.keys().contains(schema.getRawdataItemName())) {
                convertXml(rawdataMessage, resultBuilder, schema);
            }
        });
        return resultBuilder.build();
    }

    void addManifest(RawdataMessage rawdataMessage, ConversionResultBuilder resultBuilder) {
        GenericRecord manifest = new GenericRecordBuilder(manifestSchema)
          .set(FIELDNAME_DC_MANIFEST, dcManifestSchemaAdapter().newRecord(rawdataMessage, valueInterceptorChain))
          .set(FIELDNAME_CONVERTER_MANIFEST, converterManifestData())
          .build();

        resultBuilder.withRecord(FIELDNAME_MANIFEST, manifest);
    }

    GenericRecord converterManifestData() {
        Map<String, String> schemaInfo = fregSchemas.stream()
          .collect(Collectors.toMap(
            FregSchemaAdapter::getTargetItemName,
            FregSchemaAdapter::getSchemaName
          ));

        return new GenericRecordBuilder(converterManifestSchema)
          .set("schemas", schemaInfo)
          .build();
    }

    void convertXml(RawdataMessage rawdataMessage, ConversionResultBuilder resultBuilder, FregSchemaAdapter schemaAdapter) {
        byte[] data = rawdataMessage.get(schemaAdapter.getRawdataItemName());
        try (XmlToRecords records = new XmlToRecords(new ByteArrayInputStream(data), schemaAdapter.getRootElementName(), schemaAdapter.getSchema(), valueInterceptorChain)) {
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