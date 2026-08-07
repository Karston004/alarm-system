package com.karstonn.alarm.ui.actionEdit;

import android.os.Bundle;
import android.text.Editable;
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

import com.karstonn.alarm.AlarmApplication;
import com.karstonn.alarm.R;
import com.karstonn.alarm.ui.actionEdit.parameterUiBuilder.ParameterUiBuilder;
import com.karstonn.alarm.ui.phaseEdit.PhaseEditViewModel;
import com.karstonn.alarmsystem.proto.Action;
import com.karstonn.alarmsystem.proto.ActionParameter;
import com.karstonn.alarmsystem.proto.ActionValue;
import com.karstonn.alarmsystem.proto.Device;
import com.karstonn.alarmsystem.proto.DeviceCapability;
import com.karstonn.alarmsystem.proto.DeviceCapabilityKey;
import com.karstonn.alarmsystem.proto.DeviceId;
import com.karstonn.alarmsystem.proto.FileData;
import com.karstonn.alarmsystem.proto.ParameterRequirement;
import com.karstonn.alarmsystem.proto.Percentage;
import com.karstonn.alarmsystem.proto.RGBA;

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
                        new ParameterUiBuilder().generateParameterUI(
                                view,
                                selectedCapability,
                                parameterContainer,
                                actionEditVm,
                                requireContext()
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