package com.eduar.radarciudadlab.domain.port.out;

import com.eduar.radarciudadlab.domain.exception.InvalidLeadFileException;
import com.eduar.radarciudadlab.domain.model.lead.ParsedLeadFile;

public interface LeadFileParser {
    ParsedLeadFile parse(byte[] content);
}
