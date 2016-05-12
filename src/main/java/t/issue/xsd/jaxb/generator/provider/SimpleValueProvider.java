package t.issue.xsd.jaxb.generator.provider;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Random;

import javax.xml.bind.annotation.XmlEnumValue;

import org.apache.commons.lang3.RandomStringUtils;

import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;
import t.issue.xsd.jaxb.generator.ObjectGenerationException;
import t.issue.xsd.metamodel.generator.model.CompositeProperty;
import t.issue.xsd.metamodel.generator.model.Property;
import t.issue.xsd.metamodel.generator.model.SimpleProperty;
import t.issue.xsd.metamodel.generator.model.SimpleTypeConstraint;

public class SimpleValueProvider implements ValueProvider {
	protected static int defaultListSize = 5;

	protected Random random = new Random(0x443FFABC);

	@Override
	public Object getValue(SimpleProperty property, Integer propertyIndex) throws ObjectGenerationException {
		return generateValue(property);
	}

	@Override
	public long getPropertyCount(Property property) {
		return property.getMaxCount() != null ? property.getMaxCount() : defaultListSize;
	}

	@Override
	public boolean isException(Property property) {
		return false;
	}

	@Override
	public boolean isMultipleProperty(Property property) {
		return (property.getMaxCount() == null || property.getMaxCount() > 1);
	}

	@Override
	public boolean isGenerateValue(Property property) {
		return true;
	}

	@Override
	public void setNewPermutation(boolean newPermutation) {
	}

	@Override
	public Property getCompositionChoiseResult(CompositeProperty compositeProperty) {
		return compositeProperty.getProperties().get(random.nextInt(compositeProperty.getProperties().size()));
	}

	@Override
	public Random getCurrentRandom() {
		return random;
	}

	private Object generateValue(SimpleProperty property) throws ObjectGenerationException {
		if (isNumericType(property.getFieldType())) {
			return generateNumber(property.getFieldType(), property.getConstraints());
		} else if (isStringType(property.getFieldType())) {
			return generateSring(property.getFieldType(), property.getConstraints());
		} else if (isBooleanType(property.getFieldType())) {
			return generateBoolean(property.getFieldType(), property.getConstraints());
		} else if (property.getFieldType().isArray()) {
			return generateArray(property.getFieldType());
		} else if (property.getFieldType().isEnum()) {
			return generateEnum(property.getFieldType(), property.getConstraints());
		}
		throw new ObjectGenerationException("Unknown property type");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Object generateEnum(Class<?> type, SimpleTypeConstraint constraints) {
		if (constraints.getEnumeration() != null) {
			Class<? extends Enum> enumType = (Class<? extends Enum>) type;
			String enumName = constraints.getEnumeration()[random.nextInt(constraints.getEnumeration().length)];
			for (Field field : enumType.getFields()) {
				XmlEnumValue enumValue = field.getAnnotation(XmlEnumValue.class);
				if (enumValue != null && enumValue.value().equals(enumName)) {
					return Enum.valueOf(enumType, field.getName());
				}
			}
		}
		return null;
	}

	private Number generateNumber(Class<?> type, SimpleTypeConstraint constraints) {
		if (constraints != null) {
			if (constraints.getTotalDigits() != null) {
				StringBuilder number = new StringBuilder();
				long length = constraints.getTotalDigits() != null ? Long.parseLong(constraints.getTotalDigits()) : 3;
				for (int i = 0; i < length; i++) {
					number.append(random.nextInt(10));
				}
				Long value = Long.parseLong(number.toString());
				if (constraints.getMinValue() != null) {
					value = Long.parseLong(constraints.getMinValue());
				} else if (constraints.getMaxValue() != null) {
					value = Long.parseLong(constraints.getMaxValue());
				}
				return getNumericValue(type, value);
			} else if (constraints.getMaxValue() != null) {
				if (constraints.getMinValue() != null) {
					return getNumericValue(type, Long.parseLong(constraints.getMinValue()));
				} else {
					return getNumericValue(type, Long.parseLong(constraints.getMaxValue()));
				}
			}
		}
		return getNumericValue(type, random.nextInt(100));
	}

	private String generateSring(Class<?> type, SimpleTypeConstraint constraints) {
		if (constraints != null) {
			if (constraints.getEnumeration() != null) {
				return constraints.getEnumeration()[0];
			} else if (constraints.getPattern() != null) {
				String value = "";
				while (value.length() == 0) {
					value = genaratePatternValue(constraints.getPattern());
					if (constraints.getMaxLength() != null) {
						Long max = Long.parseLong(constraints.getMaxLength());
						if (value.length() > max) {
							value = value.substring(0, max.intValue());
						}
					} else if (constraints.getMinLength() != null) {
						Long min = Long.parseLong(constraints.getMinLength());
						if (value.length() < min) {
							int charCount = min.intValue() - value.length();
							if (value.length() >= charCount) {
								value += value.substring(0, charCount);
							} else {
								for (int i = 0; i < charCount; i++) {
									value += value.charAt(0);
								}
							}
						}
					}
				}
				return normalizeString(value);
			} else {
				long length = 0;
				if (constraints.getLength() != null) {
					length = Long.parseLong(constraints.getLength());
				} else if (constraints.getMaxLength() != null || constraints.getMinLength() != null) {
					length = randomInt(constraints.getMinLength() != null ? (int)Long.parseLong(constraints.getMinLength()) : null,
									constraints.getMaxLength() != null ? (int)Long.parseLong(constraints.getMaxLength()) : null);
				} else {
					length = random.nextInt(128) + 1;
				}
				return RandomStringUtils.random(random.nextInt((int) length) + 1, 0, 0, true, false, null, random);
			}
		} else {
			return RandomStringUtils.random(random.nextInt(10) + 1, 0, 0, true, false, null, random);
		}
	}

	private int randomInt(Integer min, Integer max) {
		if (min == null) {
			min = 1;
		} else {
			max = Integer.MAX_VALUE - 1;
		}
		return (random.nextInt(Math.abs(max - min) + 1)) + min;
	}

	private String normalizeString(String str) {
		StringBuilder normalaized = new StringBuilder(str);
		for (int i = 0; i < normalaized.length(); i++) {
			if (Character.isLetter(normalaized.charAt(i)) || Character.isWhitespace(normalaized.charAt(i))) {
				normalaized.setCharAt(i, RandomStringUtils.random(1, 0, 0, true, false, null, random).charAt(0));
			}
		}
		return normalaized.toString();
	}

	private Boolean generateBoolean(Class<?> type, SimpleTypeConstraint constraints) {
		return random.nextBoolean();
	}

	private Object generateArray(Class<?> type) {
		byte[] array = new byte[random.nextInt(128)];
		random.nextBytes(array);
		if (type.equals(byte[].class)) {
			return array;
		} else if (type.equals(short[].class)) {
			short[] shorts = new short[array.length];
			for (int i = 0; i < shorts.length; i++) {
				shorts[i] = array[i];
			}
			return shorts;
		} else if (type.equals(int[].class)) {
			int[] ints = new int[array.length];
			for (int i = 0; i < ints.length; i++) {
				ints[i] = array[i];
			}
			return ints;
		} else if (type.equals(long[].class)) {
			long[] longs = new long[array.length];
			for (int i = 0; i < longs.length; i++) {
				longs[i] = array[i];
			}
			return longs;
		} else if (type.equals(float[].class)) {
			float[] floats = new float[array.length];
			for (int i = 0; i < floats.length; i++) {
				floats[i] = array[i];
			}
			return floats;
		} else if (type.equals(double[].class)) {
			double[] doubles = new double[array.length];
			for (int i = 0; i < doubles.length; i++) {
				doubles[i] = array[i];
			}
			return doubles;
		} else if (type.equals(boolean[].class)) {
			boolean[] bools = new boolean[array.length];
			for (int i = 0; i < bools.length; i++) {
				bools[i] = array[i] % 2 == 0;
			}
			return bools;
		} else if (type.equals(char[].class)) {
			char[] chars = new char[array.length];
			for (int i = 0; i < chars.length; i++) {
				chars[i] = (char) array[i];
			}
			return chars;
		} else {
			return new Object[] {};
		}
	}

	private Number getNumericValue(Class<?> type, long value) {
		Number valueObj = null;
		if (type.equals(Long.class)) {
			valueObj = new Long(value);
		} else if (type.equals(Integer.class)) {
			valueObj = new Integer((int) value);
		} else if (type.equals(BigInteger.class)) {
			valueObj = new BigInteger(String.valueOf(value));
		} else if (type.equals(BigDecimal.class)) {
			valueObj = new BigDecimal(value);
		} else {
			valueObj = value;
		}
		return valueObj;
	}

	private String genaratePatternValue(String pattern) {
		StringBuilder builder = new StringBuilder();
		generate(builder, new RegExp(preparePattern(pattern)).toAutomaton().getInitialState());
		return builder.toString();
	}

	private void generate(StringBuilder builder, State state) {
		List<Transition> transitions = state.getSortedTransitions(true);
		if (transitions.size() == 0) {
			assert (state.isAccept());
			return;
		}
		int nroptions = state.isAccept() ? transitions.size() : transitions.size() - 1;
		int option = getRandomInt(0, nroptions);
		if ((state.isAccept()) && (option == 0)) {
			return;
		}

		Transition transition = (Transition) transitions.get(option - (state.isAccept() ? 1 : 0));
		appendChoice(builder, transition);
		generate(builder, transition.getDest());
	}

	private void appendChoice(StringBuilder builder, Transition transition) {
		char c = (char) getRandomInt(transition.getMin(), transition.getMax());
		builder.append(c);
	}

	private int getRandomInt(int min, int max) {
		int dif = max - min;
		float number = random.nextFloat();
		return min + Math.round(number * dif);
	}

	private String preparePattern(String pattern) {
		return pattern.replaceAll("\\w*(\\\\d)\\w*", "[0-9]");
	}

	private boolean isNumericType(Class<?> type) {
		return Number.class.isAssignableFrom(type) || byte.class.equals(type) || short.class.equals(type) || int.class.equals(type) || short.class.equals(type) || long.class.equals(type)
				|| float.class.equals(type) || double.class.equals(type);
	}

	private boolean isStringType(Class<?> type) {
		return (String.class.equals(type));
	}

	private boolean isBooleanType(Class<?> type) {
		return (type.equals(Boolean.class) || type.getName().equals(boolean.class.getName()));
	}

}
