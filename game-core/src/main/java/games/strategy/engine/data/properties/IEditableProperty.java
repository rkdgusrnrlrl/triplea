package games.strategy.engine.data.properties;

import javax.swing.JComponent;

/**
 * An editable property.
 */
public interface IEditableProperty {
  /**
   * get the name of the property.
   *
   * @return the name
   */
  String getName();

  /**
   * Get the value of the property.
   *
   * @return the value
   */
  Object getValue();

  /**
   * Indicates the object is a valid object for setting as our value.
   */
  boolean validate(Object value);

  /**
   * Set the value of the property (programmatically), GUI would normally use the editor.
   *
   * @param value the new value
   * @throws ClassCastException
   *         if the type of value is wrong
   */
  void setValue(Object value) throws ClassCastException;

  /**
   * Returns the component used to edit this property.
   */
  JComponent getEditorComponent();

  /**
   * Convenience method to get the editor component in a 'disabled' state,
   * meaning user interaction with the component is not allowed.
   */
  default JComponent getEditorComponentDisabled() {
    final JComponent component = getEditorComponent();
    component.setEnabled(false);
    return component;
  }

  /**
   * Get the view (read only) component for this property.
   */
  JComponent getViewComponent();

  /**
   * Description of what this property is, can be used for tooltip.
   */
  String getDescription();

  int getRowsNeeded();
}
