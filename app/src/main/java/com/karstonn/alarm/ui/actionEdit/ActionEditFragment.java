package com.karstonn.alarm.ui.actionEdit;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.karstonn.alarm.AlarmApplication;
import com.karstonn.alarm.R;
import com.karstonn.alarm.ui.phaseEdit.PhaseEditViewModel;
import com.karstonn.alarm.ui.util.SimpleTextWatcher;
import com.karstonn.alarmsystem.proto.Action;
import com.karstonn.alarmsystem.proto.ActionParameter;
import com.karstonn.alarmsystem.proto.ActionValue;
import com.karstonn.alarmsystem.proto.Device;
import com.karstonn.alarmsystem.proto.DeviceCapability;
import com.karstonn.alarmsystem.proto.DeviceCapabilityKey;
import com.karstonn.alarmsystem.proto.DeviceId;
import com.karstonn.alarmsystem.proto.DoubleRequirement;
import com.karstonn.alarmsystem.proto.FileData;
import com.karstonn.alarmsystem.proto.INT32Requirement;
import com.karstonn.alarmsystem.proto.ParameterRequirement;
import com.karstonn.alarmsystem.proto.Percentage;
import com.karstonn.alarmsystem.proto.RGBA;
import com.karstonn.alarmsystem.proto.StringRequirement;
import com.karstonn.alarmsystem.proto.UINT32Requirement;

import java.util.List;

public class ActionEditFragment extends Fragment {
    private static final String ARG_ACTION_INDEX = "action_index";

    private ActionEditViewModel actionEditVm;
    private PhaseEditViewModel phaseVm;
    private LinearLayout parameterContainer;
    private List<Device> devices;
    private Device selectedDevice;
    private DeviceCapability selectedCapability;


    public ActionEditFragment() {
    }


    //--------- Static Constructor ---------
    public static ActionEditFragment newInstance(int actionIndex) {
        ActionEditFragment fragment = new ActionEditFragment();

        Bundle args = new Bundle();
        args.putInt(ARG_ACTION_INDEX, actionIndex);

        fragment.setArguments(args);
        return fragment;
    }

    //--------- Fragment Creation ---------


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //--Load ViewModels--
        //Load phaseVm
        phaseVm = new ViewModelProvider(requireActivity())
                .get(PhaseEditViewModel.class);
        //Load actionVm
        actionEditVm = new ViewModelProvider(this)
                .get(ActionEditViewModel.class);

        //--Setup Device Repo--
        fetchDevices();

        //If just opened, load saved action
        //Note: Vm owner is 'this' and therefore statement is true every time an action is selected
        if (!actionEditVm.hasDraftAction()) {
            int actionIndex = requireArguments().getInt(ARG_ACTION_INDEX);
            Action action = phaseVm
                    .getDraftPhase()
                    .getActions(actionIndex);

            actionEditVm.loadAction(action);
        }

        //Identify Selected Actions
        try {
            selectedDevice = devices.get(getIndexOfDevice(actionEditVm.getDraftAction().getDeviceId()));

        } catch (IllegalArgumentException e) {
            selectedDevice = devices.get(0);
        }

        try {
            selectedCapability = selectedDevice.getCapabilities(getIndexOfCapability(actionEditVm.getDraftAction().getDeviceActionKey()));
        } catch (IllegalArgumentException e) {
            selectedCapability = selectedDevice.getCapabilities(0);
        }

    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(
                R.layout.fragment_action_edit,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        parameterContainer = view.findViewById(R.id.parameterContainer);

        setupLabel(view);
        setupDeviceSpinner(view);
        setupSaveButton(view);
        setupCancelButton(view);
        setupDeleteButton(view);
    }
    private void fetchDevices(){
        AlarmApplication application = (AlarmApplication) requireActivity().getApplication();
        actionEditVm.setDeviceRepo(application.getDeviceRepo());
        devices = actionEditVm.fetchDevices();
    }

    //--------- UI Setup Functions ---------
    private void setupSaveButton(View view) {
        view.findViewById(R.id.confirmActionButton)
                .setOnClickListener(v -> {
                    Action editedAction = actionEditVm.getDraftAction();

                    phaseVm.updatePhase(builder ->
                            builder.setActions(
                                    requireArguments().getInt(ARG_ACTION_INDEX),
                                    editedAction
                            )
                    );

                    actionEditVm.markSaved();

                   leaveActionEditor();
                });
    }

    private void setupCancelButton(View view){
        view.findViewById(R.id.cancelActionButton)
                .setOnClickListener(v ->
                    showCancelConfirmation());
    }
    private void setupDeleteButton(View view){
        view.findViewById(R.id.deleteActionButton)
                .setOnClickListener(v ->
                    showDeleteConfirmation());
    }
    private void setupLabel(View view) {
        EditText labelInput =
                view.findViewById(R.id.actionNameInput);

        labelInput.setText(
                actionEditVm.getDraftAction().getLabel()
        );

        labelInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after
            ) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count
            ) {
                actionEditVm.updateAction(builder ->
                        builder.setLabel(s.toString())
                );
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }
    private void setupDeviceSpinner(View view) {
        Spinner deviceSpinner = view.findViewById(R.id.deviceSpinner);

        ArrayAdapter<Device> adapter =
                new ArrayAdapter<Device>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        devices
                ) {
                    @NonNull
                    @Override
                    public View getView(
                            int position,
                            @Nullable View convertView,
                            @NonNull ViewGroup parent
                    ) {
                        TextView textView = (TextView) super.getView(
                                position,
                                convertView,
                                parent
                        );

                        Device device = getItem(position);

                        if (device != null) {
                            textView.setText(device.getLabel());
                        }

                        return textView;
                    }

                    @Override
                    public View getDropDownView(
                            int position,
                            @Nullable View convertView,
                            @NonNull ViewGroup parent
                    ) {
                        TextView textView = (TextView) super.getDropDownView(
                                position,
                                convertView,
                                parent
                        );

                        Device device = getItem(position);

                        if (device != null) {
                            textView.setText(device.getLabel());
                        }

                        return textView;
                    }
                };

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        deviceSpinner.setAdapter(adapter);

        deviceSpinner.setAdapter(adapter);

        // Select the existing device
        if (selectedDevice != null) {
            for (int i = 0; i < devices.size(); i++) {
                Device device = devices.get(i);

                if (device.getDeviceId().equals(selectedDevice.getDeviceId())) {
                    deviceSpinner.setSelection(i);
                    break;
                }
            }
        }

        deviceSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View selectedView,
                            int position,
                            long id
                    ) {
                         selectedDevice = (Device) parent.getItemAtPosition(position);

                        onDeviceSelected(view);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                }
        );
    }
    private void onDeviceSelected(View view) {
        setupActionTypeSpinner(view);
    }
    private void setupActionTypeSpinner(View view) {
        Spinner actionTypeSpinner =
                view.findViewById(R.id.actionTypeSpinner);

        List<DeviceCapability> capabilities =
                selectedDevice.getCapabilitiesList();

        ArrayAdapter<DeviceCapability> adapter =
                new ArrayAdapter<DeviceCapability>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        capabilities
                ) {
                    @NonNull
                    @Override
                    public View getView(
                            int position,
                            @Nullable View convertView,
                            @NonNull ViewGroup parent
                    ) {
                        TextView textView = (TextView) super.getView(
                                position,
                                convertView,
                                parent
                        );

                        DeviceCapability capability = getItem(position);

                        if (capability != null) {
                            textView.setText(capability.getLabel());
                        }

                        return textView;
                    }

                    @Override
                    public View getDropDownView(
                            int position,
                            @Nullable View convertView,
                            @NonNull ViewGroup parent
                    ) {
                        TextView textView = (TextView) super.getDropDownView(
                                position,
                                convertView,
                                parent
                        );

                        DeviceCapability capability = getItem(position);

                        if (capability != null) {
                            textView.setText(capability.getLabel());
                        }

                        return textView;
                    }
                };

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        actionTypeSpinner.setAdapter(adapter);

        // Select the existing capability
        //TODO: Tidy this up
        if (selectedCapability != null) {
            for (int i = 0; i < selectedDevice.getCapabilitiesCount(); i++) {
                DeviceCapability capability = selectedDevice.getCapabilities(i);

                if (capability.getKey().equals(selectedCapability.getKey())) {
                    actionTypeSpinner.setSelection(i);
                    break;
                }
            }
        }

        //TODO:
        // Check if this is needed
        if (capabilities.isEmpty()) {
            actionTypeSpinner.setEnabled(false);
            actionTypeSpinner.setOnItemSelectedListener(null);
            parameterContainer.removeAllViews();
            return;
        }

        actionTypeSpinner.setEnabled(true);

        actionTypeSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View selectedView,
                            int position,
                            long id
                    ) {
                        DeviceCapability selectedCapability =
                                (DeviceCapability) parent.getItemAtPosition(
                                        position
                                );

                        if (selectedCapability == null) {
                            parameterContainer.removeAllViews();
                            return;
                        }
                        generateParams(selectedCapability);
                        generateParameterUI(
                                view,
                                selectedCapability
                        );
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {
                        parameterContainer.removeAllViews();
                    }
                }
        );
    }
    private void generateParameterUI(View view, DeviceCapability deviceCapability){
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
        Context context = requireContext();

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

        //TODO: set margin
        int margin = (dpToPx(8));
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
        MaterialSwitch input = new MaterialSwitch(requireContext());

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

        TextView label = new TextView(requireContext());

        label.setText(
                requirement.getLabel()
                        + ": "
                        + initialPercentage
                        + "%"
        );

        Slider slider = new Slider(requireContext());
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

        TextView label = new TextView(requireContext());
        label.setText(requirement.getLabel());

        View colourPreview = new View(requireContext());

        LinearLayout.LayoutParams previewParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dpToPx(56)
                );

        previewParams.topMargin = dpToPx(6);
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
                dpToPx(8),
                0,
                dpToPx(8)
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
                dpToPx(10)
        );

        drawable.setStroke(
                dpToPx(1),
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
                new ColorWheelView(requireContext());

        wheel.setColor(
                rgbaToAndroidColor(initialRgba)
        );

        int size = dpToPx(280);

        wheel.setLayoutParams(
                new LinearLayout.LayoutParams(
                        size,
                        size
                )
        );

        new MaterialAlertDialogBuilder(requireContext())
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
                new LinearLayout(requireContext());

        group.setOrientation(LinearLayout.VERTICAL);

        group.setPadding(
                0,
                dpToPx(8),
                0,
                dpToPx(8)
        );

        return group;
    }
    private int dpToPx(int dp) {
        return Math.round(
                dp * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }
    //--- Confirmation Popups ---
    private void showCancelConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Discard changes?")
                .setMessage("Any changes you have made will be lost.")
                .setNegativeButton("Keep editing", null)
                .setPositiveButton("Discard", (dialog, which) ->
                        leaveActionEditor()
                )
                .show();
    }
    private void showDeleteConfirmation () {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Action?")
                .setMessage("The Action will be permanently lost.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                            phaseVm.updatePhase(builder ->
                                    builder.removeActions(requireArguments().getInt(ARG_ACTION_INDEX)));
                            leaveActionEditor();
                        }
                )
                .show();
    }
    private void leaveActionEditor(){
        getParentFragmentManager().popBackStack();
    }

    //--------- Action Object Helper Functions ---------
    private int getIndexOfDevice(DeviceId deviceId){
        for (int i = 0; i < devices.size(); i++) {
            if (devices.get(i).getDeviceId().equals(deviceId))
                return i;
        }
        throw new IllegalArgumentException(
                "No Device found of ID: " + deviceId
        );
    }
    private int getIndexOfCapability(DeviceCapabilityKey key){
        for (int i =0; i < selectedDevice.getCapabilitiesCount(); i++){
            if (selectedDevice.getCapabilities(i).getKey().equals(key))
                return i;
        }
        throw new IllegalArgumentException(
                "No Capability found with key: " + key + "\nFor device ID: " + selectedDevice.getDeviceId()
        );
    }
    private void generateParams(DeviceCapability deviceCapability) {
        Log.d("generateParams", "Check");
        if (actionEditVm.getDraftAction().getParametersCount() < deviceCapability.getParameterRequirementsCount()
                || !actionEditVm.getDraftAction().getDeviceId().equals(selectedDevice.getDeviceId())
                || !actionEditVm.getDraftAction().getDeviceActionKey().equals(deviceCapability.getKey()))
        {
            Log.d("generateParams", "Needed");
            actionEditVm.updateAction(builder -> builder
                    .setDeviceId(selectedDevice.getDeviceId())
                    .setDeviceActionKey(deviceCapability.getKey())
                    .clearParameters()
            );
            for (ParameterRequirement parameterRequirement: deviceCapability.getParameterRequirementsList()) {
                ActionParameter parameter = ActionParameter.newBuilder()
                        .setLabel(parameterRequirement.getLabel())
                        .setParameterKey(parameterRequirement.getKey())
                        .setParameterId("NULL")//TODO gen unique key
                        .setValue(blankActionValue(parameterRequirement))
                        .build();
                actionEditVm.updateAction(builder -> builder.addParameters(parameter));
            }
        }
    }
    private ActionValue blankActionValue(ParameterRequirement requirement) {
        switch (requirement.getRequirementCase()) {
            case STRING_REQUIREMENT:
                return ActionValue.newBuilder().setStringVal("Empty").build();
            case UINT32_REQUIREMENT:
                return ActionValue.newBuilder().setUint32Val(0).build();
            case INT32_REQUIREMENT:
                return ActionValue.newBuilder().setInt32Val(0).build();
            case DOUBLE_REQUIREMENT:
                return ActionValue.newBuilder().setDoubleVal(0f).build();
            case BOOL_REQUIREMENT:
                return ActionValue.newBuilder().setBoolVal(false).build();
            case RGBA_REQUIREMENT:
                return ActionValue.newBuilder().setRgbaVal(RGBA.newBuilder().setRgba(0).build()).build();
            case PERCENTAGE_REQUIREMENT:
                return ActionValue.newBuilder().setPercentage(Percentage.newBuilder().setValue(0).build()).build();
            case FILE_REQUIREMENT:
                return ActionValue.newBuilder().setFile(FileData.newBuilder().build()).build();

            case REQUIREMENT_NOT_SET:
            default:
                throw new IllegalArgumentException(
                        "Unsupported parameter type: "
                                + requirement.getRequirementCase()
                );
        }
    }

}