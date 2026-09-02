package org.javahelpers.simple.builders.example;

import static org.javahelpers.simple.builders.core.util.TrackedValue.changedValue;
import static org.javahelpers.simple.builders.core.util.TrackedValue.initialValue;
import static org.javahelpers.simple.builders.core.util.TrackedValue.unsetValue;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.javahelpers.simple.builders.core.util.BuilderToStringStyle;
import org.javahelpers.simple.builders.core.util.TrackedValue;
public class CustomerDtoBuilder {

  private TrackedValue<String> email = unsetValue();
  private TrackedValue<Long> id = unsetValue();
  private TrackedValue<String> name = unsetValue();
  private TrackedValue<List<String>> tags = unsetValue();

  public CustomerDtoBuilder() {
  }

  public CustomerDtoBuilder(CustomerDto instance) {
    this.email = initialValue(instance.getEmail());
    this.id = initialValue(instance.getId());
    this.name = initialValue(instance.getName());
    this.tags = initialValue(instance.getTags());
  }

  public static CustomerDtoBuilder create() {
    return new CustomerDtoBuilder();
  }

  public CustomerDtoBuilder email(String email) {
    this.email = changedValue(email);
    return this;
  }

  public CustomerDtoBuilder id(Long id) {
    this.id = changedValue(id);
    return this;
  }

  public CustomerDtoBuilder name(String name) {
    this.name = changedValue(name);
    return this;
  }

  public CustomerDtoBuilder tags(List<String> tags) {
    this.tags = changedValue(tags);
    return this;
  }

  CustomerDtoBuilder validateEmail() {
    if (!email.isSet() || email.value().trim().isEmpty()) {
      throw new IllegalArgumentException("Email cannot be null or empty");
    }
    return this;
  }

  CustomerDtoBuilder validateName() {
    if (!name.isSet() || name.value().trim().isEmpty()) {
      throw new IllegalArgumentException("Name cannot be null or empty");
    }
    return this;
  }

  public CustomerDto build() {
    CustomerDto result = new CustomerDto();
    this.email.ifSet(result::setEmail);
    this.id.ifSet(result::setId);
    this.name.ifSet(result::setName);
    this.tags.ifSet(result::setTags);
    return result;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, BuilderToStringStyle.INSTANCE).append("email", this.email)
        .append("id", this.id).append("name", this.name).append("tags", this.tags).toString();
  }
}