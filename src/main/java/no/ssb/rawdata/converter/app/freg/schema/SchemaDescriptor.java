package no.ssb.rawdata.converter.app.freg.schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This allows for overriding properties in a FregSchemaAdapter
 * TODO: Don't duplicate FregSchemaAdapter
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchemaDescriptor {
    private String schemaName;
    private Boolean optional;
    private String rawdataItemName;
    private String targetItemName;
    private String rootElementName;
}
