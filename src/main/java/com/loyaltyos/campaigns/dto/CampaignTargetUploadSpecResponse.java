package com.loyaltyos.campaigns.dto;

import java.util.List;

public class CampaignTargetUploadSpecResponse {

    private String format;
    private String encoding;
    private int maxFileSizeMb;
    private int maxRows;
    private List<String> standardHeaders;
    private List<CampaignTargetUploadColumnSpec> columns;
    private String exampleCsv;
    private String exampleFilename;
    private List<String> notes;

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public String getEncoding() { return encoding; }
    public void setEncoding(String encoding) { this.encoding = encoding; }
    public int getMaxFileSizeMb() { return maxFileSizeMb; }
    public void setMaxFileSizeMb(int maxFileSizeMb) { this.maxFileSizeMb = maxFileSizeMb; }
    public int getMaxRows() { return maxRows; }
    public void setMaxRows(int maxRows) { this.maxRows = maxRows; }
    public List<String> getStandardHeaders() { return standardHeaders; }
    public void setStandardHeaders(List<String> standardHeaders) { this.standardHeaders = standardHeaders; }
    public List<CampaignTargetUploadColumnSpec> getColumns() { return columns; }
    public void setColumns(List<CampaignTargetUploadColumnSpec> columns) { this.columns = columns; }
    public String getExampleCsv() { return exampleCsv; }
    public void setExampleCsv(String exampleCsv) { this.exampleCsv = exampleCsv; }
    public String getExampleFilename() { return exampleFilename; }
    public void setExampleFilename(String exampleFilename) { this.exampleFilename = exampleFilename; }
    public List<String> getNotes() { return notes; }
    public void setNotes(List<String> notes) { this.notes = notes; }
}
