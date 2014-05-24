/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.plugin.idea.ui.component;

import org.jboss.forge.addon.convert.Converter;
import org.jboss.forge.addon.convert.ConverterFactory;
import org.jboss.forge.addon.ui.hints.InputType;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.util.InputComponents;
import org.jboss.forge.plugin.idea.service.ServiceHelper;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

public class PasswordComponentBuilder extends ComponentBuilder
{

    @Override
    public JComponent build(final InputComponent<?, Object> input,
                            Container container)
    {
        final JTextField textField = new JPasswordField();
        // Set Default Value
        final ConverterFactory converterFactory = ServiceHelper.getForgeService()
                .lookup(ConverterFactory.class);
        Converter<Object, String> converter = converterFactory.getConverter(
                input.getValueType(), String.class);
        String value = converter.convert(InputComponents.getValueFor(input));
        textField.setText(value == null ? "" : value);

        textField.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void removeUpdate(DocumentEvent e)
            {
                InputComponents.setValueFor(converterFactory, input,
                        textField.getText());
            }

            @Override
            public void insertUpdate(DocumentEvent e)
            {
                InputComponents.setValueFor(converterFactory, input,
                        textField.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
                InputComponents.setValueFor(converterFactory, input,
                        textField.getText());
            }
        });
        String labelValue = input.getLabel() == null ? input.getName() : input
                .getLabel();
        container.add(new JLabel(labelValue));
        container.add(textField);
        return textField;
    }

    @Override
    protected Class<String> getProducedType()
    {
        return String.class;
    }

    @Override
    protected String getSupportedInputType()
    {
        return InputType.SECRET;
    }

    @Override
    protected Class<?>[] getSupportedInputComponentTypes()
    {
        return new Class<?>[]{UIInput.class};
    }

}
