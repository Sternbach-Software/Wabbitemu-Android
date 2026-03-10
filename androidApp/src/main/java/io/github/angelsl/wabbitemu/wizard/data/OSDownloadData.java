package io.github.angelsl.wabbitemu.wizard.data;


import io.github.angelsl.wabbitemu.calc.CalcModel;

import javax.annotation.Nonnull;

public class OSDownloadData {
    public final CalcModel mCalcModel;
    public final String mOsUrl;

    public OSDownloadData(@Nonnull CalcModel calcModel, @Nonnull String osUrl) {
        mCalcModel = calcModel;
        mOsUrl = osUrl;
    }
}
