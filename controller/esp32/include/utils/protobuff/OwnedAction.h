#pragma once

#include <vector>
#include "alarm.pb.h"

class OwnedAction
{
public:
    explicit OwnedAction(
        const alarm_system_v1_Action &source)
        : action(source)
    {
        if (source.parameters_count > 0)
        {
            parameters.assign(
                source.parameters,
                source.parameters + source.parameters_count);
        }

        action.parameters =
            parameters.empty() ? nullptr : parameters.data();
    }

    const alarm_system_v1_Action &get() const
    {
        return action;
    }

    // Prevent copies/moves that would invalidate
    // the internal parameters pointer.
    OwnedAction(const OwnedAction &) = delete;
    OwnedAction &operator=(const OwnedAction &) = delete;
    OwnedAction(OwnedAction &&) = delete;
    OwnedAction &operator=(OwnedAction &&) = delete;

private:
    alarm_system_v1_Action action;
    std::vector<alarm_system_v1_ActionParameter> parameters;
};
