package dk.pfrandsen.check;

import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SerializationTest {

    private static URI getLocation(String dataFile) throws URISyntaxException {
        return SerializationTest.class.getResource(dataFile).toURI();
    }

    @Test
    public void testSerialize() throws IOException {
        AnalysisInformationCollector collector = new AnalysisInformationCollector();
        collector.addError("Assertion id 1", "Error message 1", AnalysisInformationCollector.SEVERITY_LEVEL_CRITICAL);
        collector.addError("Assertion id 2", "Error message 2", AnalysisInformationCollector.SEVERITY_LEVEL_MAJOR);
        String json = collector.toJson(false);
        assertTrue(json.contains("\"errors\":[{\"assertion\":\"Assertion id 1\""));
        assertTrue(json.endsWith("\"info\":[],\"warnings\":[]}"));
        assertTrue(json.contains(",{\"assertion\":\"Assertion id 2\",\"details\":\"\",\"message\"" +
                ":\"Error message 2\",\"severity\":2}],"));
    }

    @Test
    public void testCollectorParser() throws IOException {
        String json = "{\"errors\":" +
                "[{\"assertion\":\"Assertion id 1\",\"details\":\"\",\"message\":\"Error message 1\",\"severity\":1}," +
                "{\"assertion\":\"Assertion id 2\",\"details\":\"\",\"message\":\"Error message 2\",\"severity\":2}]," +
                "\"info\":[],\"warnings\":[]}";
        AnalysisInformationCollector collector = AnalysisInformationCollector.fromJson(json);
        assertEquals(2, collector.errorCount());
        assertEquals(0, collector.warningCount());
        assertEquals(0, collector.infoCount());
        assertEquals(AnalysisInformationCollector.SEVERITY_LEVEL_MAJOR, collector.getErrors().get(1).getSeverity());
    }

    @Test
    public void testCollectorParseFromFile() throws IOException, URISyntaxException {
        URI jsonFile = getLocation("/json/serialized.json");
        AnalysisInformationCollector collector =
                AnalysisInformationCollector.fromJson(new FileInputStream(Paths.get(jsonFile).toFile()));
        assertEquals(5, collector.errorCount());
        assertEquals(2, collector.warningCount());
        assertEquals(1, collector.infoCount());
        assertEquals(AnalysisInformationCollector.SEVERITY_LEVEL_UNKNOWN, collector.getWarnings().get(0).getSeverity());
        assertEquals(AnalysisInformationCollector.SEVERITY_LEVEL_MAJOR, collector.getWarnings().get(1).getSeverity());
        assertEquals("Info message 1", collector.getInfo().get(0).getMessage());
    }

    @Test
    public void testCollectorParseEmptyFromFile() throws IOException, URISyntaxException {
        URI jsonFile = getLocation("/json/empty.json");
        AnalysisInformationCollector collector =
                AnalysisInformationCollector.fromJson(new FileInputStream(Paths.get(jsonFile).toFile()));
        assertEquals(0, collector.errorCount());
        assertEquals(0, collector.warningCount());
        assertEquals(0, collector.infoCount());
    }

    @Test
    public void testSimple() throws IOException {
        AnalysisInformation info = new AnalysisInformation("Assertion id", "Message",
                AnalysisInformationCollector.SEVERITY_LEVEL_MINOR);
        String json = info.toJson(false);
        assertEquals("{\"assertion\":\"Assertion id\",\"details\":\"\",\"message\":\"Message\",\"severity\":3}", json);
    }

    @Test
    public void testSimpleDetails() throws IOException {
        AnalysisInformation info = new AnalysisInformation("id", "msg",
                AnalysisInformationCollector.SEVERITY_LEVEL_MINOR, "detail msg");
        String json = info.toJson(true);
        assertEquals("{\"assertion\":\"id\",\"details\":\"detail msg\",\"message\":\"msg\",\"severity\":3}", json);
    }

    @Test
    public void testParseSimple() throws IOException {
        String json = "{\"assertion\":\"Assertion id\",\"details\":\"dtl\",\"message\":\"Message\",\"severity\":3}";
        AnalysisInformation info = AnalysisInformation.fromJson(json);
        assertEquals("Assertion id", info.getAssertion());
        assertEquals("Message", info.getMessage());
        assertEquals(AnalysisInformationCollector.SEVERITY_LEVEL_MINOR, info.getSeverity());
        assertEquals("dtl", info.getDetails());
    }

}
