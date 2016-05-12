package t.issue.xsd.jaxb.generator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import t.issue.xsd.jaxb.generator.provider.SimpleValueProvider;
import t.issue.xsd.metamodel.generator.XsdMetamodelGenerator;
import t.issue.xsd.metamodel.generator.model.ComplexType;
import t.issue.xsd.metamodel.generator.test.model.Model;
import t.issue.xsd.metamodel.generator.test.model.ObjectFactory;

/**
 * TODO write comprehensive tests.
 *
 * @author Nezhull
 *
 */
public class JaxbObjectGeneratorTest {
	private static final Log LOG = LogFactory.getLog(JaxbObjectGeneratorTest.class);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGeneratePermutations() throws IOException, JAXBException, SAXException {
		XsdMetamodelGenerator metamodelGenerator = new XsdMetamodelGenerator();

		ComplexType type = null;

		try (InputStream xsdStream = this.getClass().getResourceAsStream("/schemas/maven-v4_0_0.xsd")) {
			type = metamodelGenerator.generateMetamodel(xsdStream, Model.class);
		}

		JAXBContext jaxbContext = JAXBContext.newInstance(Model.class);
		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

		ObjectFactory objectFactory = new ObjectFactory();

		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = schemaFactory.newSchema(this.getClass().getResource("/schemas/maven-v4_0_0.xsd"));

		// enabling schema validation
		jaxbMarshaller.setSchema(schema);

		JaxbObjectGenerator objectGenerator = new JaxbObjectGenerator(new SimpleValueProvider());

		objectGenerator.generatePermutations(type, (obj) -> {
			Model model = (Model) obj;

			try {
				jaxbMarshaller.marshal(objectFactory.createProject(model), new OutputStream() {
					@Override
					public void write(int b) throws IOException {
						// do nothing
					}
				});
			} catch (JAXBException e) {
				LOG.error(e.getMessage(), e);
			}
		});
	}

}
