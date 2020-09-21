package com.craws.mrx.engine;

import com.craws.mrx.MVVM.StateModel;

public interface PhaseChangeListener {
    void onPhaseChange(final StateModel.GAME_PHASE phase);
}
