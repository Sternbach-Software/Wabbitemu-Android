package io.github.angelsl.wabbitemu.calc;


public enum CalcModel {
    NO_CALC(CalcInterface.NO_CALC),
    TI_81(CalcInterface.TI_81),
    TI_82(CalcInterface.TI_82),
    TI_83(CalcInterface.TI_83),
    TI_85(CalcInterface.TI_85),
    TI_86(CalcInterface.TI_86),
    TI_73(CalcInterface.TI_73),
    TI_83P(CalcInterface.TI_83P),
    TI_83PSE(CalcInterface.TI_83PSE),
    TI_84P(CalcInterface.TI_84P),
    TI_84PSE(CalcInterface.TI_84PSE),
    TI_84PCSE(CalcInterface.TI_84PCSE);

    private final int mCalcInterfaceModel;

    CalcModel(int calcInterfaceModel) {
        mCalcInterfaceModel = calcInterfaceModel;
    }

    public static CalcModel fromModel(int model) {
        for (CalcModel calcModel : values()) {
            if (calcModel.mCalcInterfaceModel == model) {
                return calcModel;
            }
        }

        throw new IllegalStateException("Invalid model " + model);
    }

    public int getModelInt() {
        return mCalcInterfaceModel;
    }
}
