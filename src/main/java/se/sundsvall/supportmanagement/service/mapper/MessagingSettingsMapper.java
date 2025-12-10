package se.sundsvall.supportmanagement.service.mapper;

public class MessagingSettingsMapper {

	private static final String FILTER_TEMPLATE = "exists(values.key: 'namespace' and values.value: '%s') and exists(values.key: 'department_name' and values.value: '%s')";

	private MessagingSettingsMapper() {
		// Private constructor to prevent instantiation
	}

	public static String toFilterString(final String namespace, final String departmentName) {
		return FILTER_TEMPLATE.formatted(namespace, departmentName);
	}

}
