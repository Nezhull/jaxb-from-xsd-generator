package t.issue.xsd.jaxb.generator.provider;

import java.util.Random;

import t.issue.xsd.jaxb.generator.ObjectGenerationException;
import t.issue.xsd.metamodel.generator.model.CompositeProperty;
import t.issue.xsd.metamodel.generator.model.Property;
import t.issue.xsd.metamodel.generator.model.SimpleProperty;

public interface ValueProvider {

	public Object getValue(SimpleProperty property, Integer propertyIndex) throws ObjectGenerationException;

	public Property getCompositionChoiseResult(CompositeProperty compositeProperty);

	public long getPropertyCount(Property property);

	public boolean isGenerateValue(Property property);

	public void setNewPermutation(boolean newPermutation);

	public boolean isException(Property property);

	public boolean isMultipleProperty(Property property);

	public Random getCurrentRandom();
}
