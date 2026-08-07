package com.karstonn.alarm.ui.actionEdit.parameterUiBuilder;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.karstonn.alarm.ui.actionEdit.ActionEditViewModel;
import com.karstonn.alarm.ui.util.SimpleTextWatcher;
import com.karstonn.alarmsystem.proto.ActionParameter;
import com.karstonn.alarmsystem.proto.ActionValue;
import com.karstonn.alarmsystem.proto.DeviceCapability;
import com.karstonn.alarmsystem.proto.DoubleRequirement;
import com.karstonn.alarmsystem.proto.INT32Requirement;
import com.karstonn.alarmsystem.proto.ParameterRequirement;
import com.karstonn.alarmsystem.proto.Percentage;
import com.karstonn.alarmsystem.proto.RGBA;
import com.karstonn.alarmsystem.proto.StringRequirement;
import com.karstonn.alarmsystem.proto.UINT32Requirement;

public class ParameterUiBuilder {
    private ActionEditViewModel actionEditVm;
    private Context context;
    private LinearLayout parameterContainer;
    public void generateParameterUI(View view, DeviceCapability deviceCapability, LinearLayout parameterContainer, ActionEditViewModel actionEditVm, Context context){
        this.actionEditVm = actionEditVm;
        this.context = context;
        this.parameterContainer = parameterContainer;
        parameterContainer.removeAllViews();
        for (ParameterRequirement parameterRequirement: deviceCapability.getParameterRequirementsList()) {
            addParameterInput(parameterRequirement);
        }
    }
    private void addParameterInput(ParameterRequirement parameterRequirement) {
        int parameterIndex = actionEditVm.findParameterIndexByKey(parameterRequirement.getKey());
        switch (parameterRequirement.getRequirementCase()) {
            case STRING_REQUIREMENT:
                addTextInput(
                        parameterRequirement,
                        InputType.TYPE_CLASS_TEXT,
                        actionEditVm.getDraftAction().getParameters(parameterIndex).getValue().getStringVal()
                );
                return;

            case UINT32_REQUIREMENT:
                addTextInput(
                        parameterRequirement,
                        InputType.TYPE_CLASS_NUMBER,
                        Integer.toString(actionEditVm.getDraftAction().getParameters(parameterIndex).getValue().getUint32Val())

                );
                return;

            case INT32_REQUIREMENT:
                addTextInput(
                        parameterRequirement,
                        InputType.TYPE_CLASS_NUMBER
                                | InputType.TYPE_NUMBER_FLAG_SIGNED,
                        Integer.toString(actionEditVm.getDraftAction().getParameters(parameterIndex).getValue().getInt32Val())
                );
                return;

            case DOUBLE_REQUIREMENT:
                addTextInput(
                        parameterRequirement,
                        InputType.TYPE_CLASS_NUMBER
                                | InputType.TYPE_NUMBER_FLAG_SIGNED
                                | InputType.TYPE_NUMBER_FLAG_DECIMAL,
                        Double.toString(actionEditVm.getDraftAction().getParameters(parameterIndex).getValue().getDoubleVal())

                );
                return;

            case BOOL_REQUIREMENT:
                addToggleSwitch(
                        parameterRequirement,
                        actionEditVm.getDraftAction().getParameters(parameterIndex).getValue().getBoolVal()
                );
                return;

            case RGBA_REQUIREMENT:
                addRgbaInput(
                        parameterRequirement,
                        actionEditVm.getDraftAction().getParameters(parameterIndex).getValue().getRgbaVal()
                );
                return;

            case PERCENTAGE_REQUIREMENT:
                addPercentageInput(
                        parameterRequirement,
                        actionEditVm.getDraftAction().getParameters(parameterIndex).getValue().getPercentage()
                );
                return;

            case FILE_REQUIREMENT:
                // TODO: Add a file picker.
                return;

            case REQUIREMENT_NOT_SET:
            default:
                throw new IllegalArgumentException(
                        "Unsupported parameter type: "
                                + parameterRequirement.getRequirementCase()
                );
        }
    }
    private void addTextInput(
            ParameterRequirement requirement,
            int inputType,
            String startingText){

        TextInputLayout inputLayout = new TextInputLayout(
                context,
                null,
                com.google.android.material.R.attr.textInputOutlinedStyle
        );
        inputLayout.setHint(requirement.getLabel());

        TextInputEditText editText = new TextInputEditText(context);
        editText.setInputType(inputType);
        editText.setSingleLine(true);
        editText.setText(startingText);

        inputLayout.addView(
                editText,
                new TextInputLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        LinearLayout.LayoutParams layoutParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        int margin = 8;
        layoutParams.setMargins(10, margin, 10, margin);

        parameterContainer.addView(inputLayout, layoutParams);

        editText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                updateTextParameter(
                        requirement,
                        editable.toString(),
                        inputLayout
                );
            }
        });
    }
    private void updateTextParameter(
            ParameterRequirement requirement,
            String text,
            TextInputLayout inputLayout
    ) {
        boolean validInput = false;
        try {
            ActionValue.Builder valueBuilder = ActionValue.newBuilder();

            switch (requirement.getRequirementCase()) {
                case STRING_REQUIREMENT:
                    StringRequirement sr = requirement.getStringRequirement();

                    if (text.length() <= sr.getMaxLength()
                            &&  text.length() >= sr.getMinLength()) {
                        valueBuilder.setStringVal(text);
                        validInput = true;
                    }
                    break;

                case UINT32_REQUIREMENT:
                    UINT32Requirement ur = requirement.getUint32Requirement();
                    long unsignedValue = Long.parseLong(text);

                    if (unsignedValue >= ur.getMinVal()
                            &&  unsignedValue <= ur.getMaxVal()) {
                        valueBuilder.setUint32Val((int) unsignedValue);
                        validInput = true;

                    }
                    break;

                case INT32_REQUIREMENT:
                    INT32Requirement ir = requirement.getInt32Requirement();
                    int singedValue = Integer.parseInt(text);
                    if(singedValue <= ir.getMaxVal()
                            && singedValue >= ir.getMinVal()){
                        valueBuilder.setInt32Val(singedValue);
                        validInput = true;
                    }
                    break;

                case DOUBLE_REQUIREMENT:
                    DoubleRequirement dr = requirement.getDoubleRequirement();
                    double doubleValue = Double.parseDouble(text);

                    if (doubleValue <= dr.getMaxVal()
                            &&  doubleValue >= dr.getMinVal()){
                        valueBuilder.setDoubleVal(doubleValue);
                        validInput = true;
                    }
                    break;

                default:
                    throw new IllegalArgumentException(
                            "Text input used for Unsupported parameter type: " + requirement.getRequirementCase());
            }

            if (validInput){
                inputLayout.setError(null);
                updateParameter(requirement, valueBuilder.build());
            } else {
                inputLayout.setError("Invalid: not in range of accepted values");
            }
        } catch (NumberFormatException exception) {
            inputLayout.setError("Invalid " + requirement.getLabel());
        }
    }
    private void addToggleSwitch(
            ParameterRequirement requirement,
            boolean startingVal
    ){
        MaterialSwitch input = new MaterialSwitch(context);

        input.setText(requirement.getLabel());
        input.setChecked(startingVal);

        input.setOnCheckedChangeListener((button, isChecked) -> {
            ActionValue value = ActionValue.newBuilder()
                    .setBoolVal(isChecked)
                    .build();

            updateParameter(requirement, value);
        });

        parameterContainer.addView(
                input,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );
    }
    private void addPercentageInput(
            ParameterRequirement requirement,
            Percentage percentage
    ) {
        int initialPercentage = percentage.getValue();
        LinearLayout group = createVerticalGroup();

        TextView label = new TextView(context);

        label.setText(
                requirement.getLabel()
                        + ": "
                        + initialPercentage
                        + "%"
        );

        Slider slider = new Slider(context);
        int minVal = requirement.getPercentageRequirement().getMinVal();
        int maxVal = requirement.getPercentageRequirement().getMaxVal();
        slider.setValueFrom(minVal);
        slider.setValueTo(maxVal);
        slider.setStepSize(requirement.getPercentageRequirement().getStep());
        slider.setValue(initialPercentage);

        slider.setLabelFormatter(value ->
                value + "%"
        );

        slider.addOnChangeListener((changedSlider, value, fromUser) -> {
            label.setText(
                    requirement.getLabel()
                            + ": "
                            + Math.round(value)
            );

            if (!fromUser) {
                return;
            }

            ActionValue actionValue = ActionValue.newBuilder()
                    .setPercentage(
                            Percentage.newBuilder()
                                    .setValue(Math.round(value))
                                    .build()
                    )
                    .build();

            updateParameter(requirement, actionValue);
        });

        group.addView(label);
        group.addView(slider);

        parameterContainer.addView(
                group,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );
    }
    private void addRgbaInput(
            ParameterRequirement requirement,
            RGBA rgba
    ) {
        LinearLayout group = createVerticalGroup();

        TextView label = new TextView(context);
        label.setText(requirement.getLabel());

        View colourPreview = new View(context);

        LinearLayout.LayoutParams previewParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        100
                );

        previewParams.topMargin = 6;
        colourPreview.setLayoutParams(previewParams);

        int initialRgba = rgba.getRgba();

        // Give the preview rounded corners + outline
        setColourPreview(
                colourPreview,
                initialRgba
        );

        // Store the CURRENT colour in the view itself.
        // This means reopening the picker uses the last selected colour.
        colourPreview.setTag(initialRgba);

        colourPreview.setOnClickListener(v -> {
            int currentRgba = (int) colourPreview.getTag();

            showColourPicker(
                    requirement,
                    currentRgba,
                    colourPreview
            );
        });

        group.addView(label);
        group.addView(colourPreview);

        LinearLayout.LayoutParams groupParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        groupParams.setMargins(
                0,
                30,
                0,
                30
        );

        parameterContainer.addView(
                group,
                groupParams
        );
    }
    private void setColourPreview(
            View preview,
            int rgba
    ) {
        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setShape(
                GradientDrawable.RECTANGLE
        );

        drawable.setColor(
                rgbaToAndroidColor(rgba)
        );

        drawable.setCornerRadius(
                10
        );

        drawable.setStroke(
                1,
                Color.GRAY
        );

        preview.setBackground(drawable);
    }
    private void showColourPicker(
            ParameterRequirement requirement,
            int initialRgba,
            View colourPreview
    ) {
        ColorWheelView wheel =
                new ColorWheelView(context);

        wheel.setColor(
                rgbaToAndroidColor(initialRgba)
        );

        int size = 280;

        wheel.setLayoutParams(
                new LinearLayout.LayoutParams(
                        size,
                        size
                )
        );

        new MaterialAlertDialogBuilder(context)
                .setTitle(requirement.getLabel())
                .setView(wheel)

                .setNegativeButton(
                        "Cancel",
                        null
                )

                .setPositiveButton(
                        "OK",
                        (dialog, which) -> {

                            int androidColour =
                                    wheel.getColor();

                            int rgba =
                                    androidColorToRgba(
                                            androidColour
                                    );

                            // Update visible swatch
                            setColourPreview(
                                    colourPreview,
                                    rgba
                            );

                            // Remember the new colour
                            colourPreview.setTag(rgba);

                            // Update action
                            ActionValue value =
                                    ActionValue.newBuilder()
                                            .setRgbaVal(
                                                    RGBA.newBuilder()
                                                            .setRgba(rgba)
                                                            .build()
                                            )
                                            .build();

                            updateParameter(
                                    requirement,
                                    value
                            );
                        }
                )

                .show();
    }
    private static int androidColorToRgba(int color) {
        int a = Color.alpha(color);
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        return ((r & 0xFF) << 24)
                | ((g & 0xFF) << 16)
                | ((b & 0xFF) << 8)
                | (a & 0xFF);
    }
    private static int rgbaToAndroidColor(int rgba) {
        int r = (rgba >>> 24) & 0xFF;
        int g = (rgba >>> 16) & 0xFF;
        int b = (rgba >>> 8) & 0xFF;
        int a = rgba & 0xFF;

        return Color.argb(a, r, g, b);
    }
    private void updateParameter(
            ParameterRequirement requirement,
            ActionValue value
    ) {
        int index = actionEditVm.findParameterIndexByKey(requirement.getKey());
        ActionParameter.Builder parameterBuilder = ActionParameter.newBuilder(actionEditVm.getDraftAction().getParameters(index));
        parameterBuilder.setValue(value);

        actionEditVm.updateAction(builder ->
                builder.setParameters(index, parameterBuilder.build())
        );
    }
    private LinearLayout createVerticalGroup() {
        LinearLayout group =
                new LinearLayout(context);

        group.setOrientation(LinearLayout.VERTICAL);

        group.setPadding(
                0,
                8,
                0,
                8
        );
        return group;
    }
}
