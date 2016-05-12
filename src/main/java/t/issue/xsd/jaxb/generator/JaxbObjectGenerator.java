package t.issue.xsd.jaxb.generator;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import t.issue.xsd.jaxb.generator.handler.ObjectGeneratedEventHandler;
import t.issue.xsd.jaxb.generator.provider.ValueProvider;
import t.issue.xsd.metamodel.generator.model.ComplexProperty;
import t.issue.xsd.metamodel.generator.model.ComplexType;
import t.issue.xsd.metamodel.generator.model.CompositeProperty;
import t.issue.xsd.metamodel.generator.model.CompositionType;
import t.issue.xsd.metamodel.generator.model.Property;
import t.issue.xsd.metamodel.generator.model.SimpleProperty;
import t.issue.xsd.metamodel.generator.utils.ReflectionUtils;

public class JaxbObjectGenerator {
	private static final Log log = LogFactory.getLog(JaxbObjectGenerator.class);

	private ValueProvider valueProvider;

	private Map<CompositeProperty, ChoisePermutation> choises = new HashMap<CompositeProperty, ChoisePermutation>();

	public JaxbObjectGenerator(ValueProvider valueProvider) {
		if (valueProvider == null) {
			throw new IllegalArgumentException("value provider can not be null");
		}
		this.valueProvider = valueProvider;
	}

	public void generatePermutations(ComplexType type, ObjectGeneratedEventHandler handler) {
		if (type == null) {
			throw new IllegalArgumentException("complex type is required");
		}
		if (handler == null) {
			throw new IllegalArgumentException("event handler is required");
		}
		Integer permutationCount = getPermutationCount(type.getContents());
		for (int i = 0; i < permutationCount; i++) {
			try {
				valueProvider.setNewPermutation(true);
				handler.onObjectGenerated(generate(type, null));
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	private int getPermutationCount(Property property) {
		int permutationCount = 0;
		if (property instanceof SimpleProperty) {
			permutationCount += property.isRequired() || valueProvider.isException(property) ? 0 : 2;
		} else if (property instanceof ComplexProperty) {
			permutationCount += property.isRequired() || valueProvider.isException(property) ? 0 : 1;
			int count = getPermutationCount(((ComplexProperty) property).getComplexType().getContents());
			permutationCount += !property.isRequired() && !valueProvider.isException(property) && count == 0 ? 1 : count;
		} else if (property instanceof CompositeProperty) {
			CompositeProperty compositeProperty = (CompositeProperty) property;
			if (CompositionType.CHOISE.equals(compositeProperty.getType())) {
				permutationCount += compositeProperty.getProperties().size() - 1;
			}
			for (Property prop : compositeProperty.getProperties()) {
				if (!valueProvider.isException(prop)) {
					permutationCount += getPermutationCount(prop);
				}
			}
		}
		return permutationCount;
	}

	private Object generate(ComplexType type, Integer index) throws InstantiationException, IllegalAccessException, IllegalArgumentException, ObjectGenerationException {
		List<Field> fields = ReflectionUtils.getAllFields(type.getTypeClass());
		Object instance = type.getTypeClass().newInstance();
		if (type.getValue() != null) {
			SimpleProperty value = type.getValue();
			if (!valueProvider.isException(value)) {
				Field field = ReflectionUtils.findField(value.getFieldName(), fields);
				field.setAccessible(true);
				field.set(instance, processSimpleProperty(value, index));
			}
		}
		for (SimpleProperty property : type.getAttributes()) {
			if (!valueProvider.isException(property)) {
				Field field = ReflectionUtils.findField(property.getFieldName(), fields);
				field.setAccessible(true);
				field.set(instance, processSimpleProperty(property, index));
			}
		}
		if (type.getContents() != null) {
			processCompositeProperty(instance, fields, type.getContents(), index);
		}
		return instance;
	}

	private void generateContents(Object instance, List<Field> fields, Property property, Integer index) throws IllegalArgumentException, IllegalAccessException, InstantiationException, ObjectGenerationException {
		if (!valueProvider.isException(property)) {
			if (property instanceof SimpleProperty) {
				SimpleProperty simpleProperty = (SimpleProperty) property;
				Field field = ReflectionUtils.findField(simpleProperty.getFieldName(), fields);
				field.setAccessible(true);
				if (valueProvider.isGenerateValue(property)) {
					field.set(instance, processSimpleProperty(simpleProperty, index));
				} else {
					field.set(instance, null);
				}
			} else if (property instanceof CompositeProperty) {
				CompositeProperty compositeProperty = (CompositeProperty) property;
				processCompositeProperty(instance, fields, compositeProperty, index);
			} else if (property instanceof ComplexProperty) {
				ComplexProperty complexProperty = (ComplexProperty) property;
				Field field = ReflectionUtils.findField(complexProperty.getFieldName(), fields);
				field.setAccessible(true);
				if (valueProvider.isGenerateValue(property)) {
					field.set(instance, processComplexProperty(complexProperty, index));
				} else {
					field.set(instance, null);
				}
			}
		}
	}

	private Object processSimpleProperty(SimpleProperty property, Integer index) throws ObjectGenerationException {
		if (valueProvider.isMultipleProperty(property)) {
			List<Object> list = new ArrayList<Object>();
			for (int i = 0; i < valueProvider.getPropertyCount(property); i++) {
				list.add(valueProvider.getValue(property, i));
			}
			return list;
		} else {
			return valueProvider.getValue(property, index);
		}
	}

	private Object processComplexProperty(ComplexProperty property, Integer index) throws ObjectGenerationException, IllegalArgumentException, InstantiationException, IllegalAccessException {
		if (valueProvider.isMultipleProperty(property)) {
			List<Object> list = new ArrayList<Object>();
			for (int i = 0; i < valueProvider.getPropertyCount(property); i++) {
				list.add(generate(property.getComplexType(), i));
			}
			return CollectionUtils.isNotEmpty(list) ? list : null;
		} else {
			return generate(property.getComplexType(), index);
		}
	}

	private void processCompositeProperty(Object instance, List<Field> fields, CompositeProperty property, Integer index) throws IllegalAccessException, InstantiationException, ObjectGenerationException {
		if (CompositionType.CHOISE.equals(property.getType())) {
			Property prop = null;
			ChoisePermutation choise = choises.get(property);
			if (choise != null && choise.getCount() > 0) {
				prop = choise.getChoise();
				choise.decCount();
			} else {
				prop = valueProvider.getCompositionChoiseResult(property);
				choises.put(property, new ChoisePermutation(getPermutationCount(prop) - 1, prop));
			}
			generateContents(instance, fields, prop, index);
		} else {
			for (Property prop : property.getProperties()) {
				generateContents(instance, fields, prop, index);
			}
		}
	}

	public void setValueProvider(ValueProvider valueProvider) {
		this.valueProvider = valueProvider;
	}

	public ValueProvider getValueProvider() {
		return valueProvider;
	}

	private class ChoisePermutation {
		private Property choise;
		private int count = 0;

		public ChoisePermutation(int count, Property choise) {
			this.count = count;
			this.choise = choise;
		}

		public void decCount() {
			if (count != 0) {
				count -= 1;
			}
		}

		public int getCount() {
			return count;
		}

		public Property getChoise() {
			return choise;
		}

	}

}
