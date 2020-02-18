package org.firstinspires.ftc.teamcode;


import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import org.openftc.revextensions2.ExpansionHubMotor;
import org.openftc.revextensions2.ExpansionHubServo;
import org.openftc.revextensions2.RevBulkData;

public class LiftManager2 {
    public ExpansionHubMotor LeftLift;
    public ExpansionHubMotor RightLift;
    public ExpansionHubMotor SlideEncoder;
    public ExpansionHubServo Elbow;
    public double liftTargetIN = 0;
    public double slideTargetIN = 0;
    public boolean isBusy = false;
    public double liftPower = 1;
    public double pidPower = 0.1;

    // Fixed ticks per inch (big oof, really sorry about that)
    public double LiftTicksPerInch = RobotConstants.LiftMotorTicksPerRotationofOuputShaft / (1.25 * Math.PI);     // for gobilda 13.7:1 and 1.25 inch spool
    public double SlideTicksPerInch = 360 / (1.5 * Math.PI);      // for vex optical shaft encoder and 1.5 inch spool

    public double tolerance = 0.25;
    public double LiftPositionIN = 0;
    public double SlidePositionIN = 0;

    public boolean liftObstruction = false;
    public boolean slideObstruction = false;

    public LiftManager2(ExpansionHubMotor LeftLift, ExpansionHubMotor RightLift, ExpansionHubServo Elbow, ExpansionHubMotor slideEncoder) {
        this.LeftLift = LeftLift;
        this.RightLift = RightLift;
        this.Elbow = Elbow;
        this.SlideEncoder = slideEncoder;
    }

    public void pause() {
        int target = Math.max(LeftLift.getCurrentPosition(), RightLift.getCurrentPosition());
        LeftLift.setTargetPosition(target);
        LeftLift.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        LeftLift.setPower(pidPower);
        RightLift.setTargetPosition(target);
        RightLift.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        RightLift.setPower(pidPower);
        liftPower = pidPower;
        Elbow.setPosition(0.5);
    }

    public void stop() {
        LeftLift.setPower(0);
        RightLift.setPower(0);
    }

    public void reset() {
        RightLift.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        LeftLift.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        RightLift.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        LeftLift.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }

    public void start() {
        RightLift.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        LeftLift.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        LiftPositionIN = Math.PI * 1.25 * Math.max(LeftLift.getCurrentPosition(), RightLift.getCurrentPosition()) / RobotConstants.LiftMotorTicksPerRotationofOuputShaft;
        liftTargetIN = LiftPositionIN;
        LeftLift.setPower(0);
        RightLift.setPower(0);
    }

    public void start(double liftTargetIN) {
        RightLift.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        LeftLift.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        this.liftTargetIN = liftTargetIN;
        isBusy = true;
        LeftLift.setPower(0);
        RightLift.setPower(0);
    }

    public void update(RevBulkData bulkData2) {


        LiftPositionIN = Math.PI * 1.25 * Math.max(LeftLift.getCurrentPosition(), RightLift.getCurrentPosition()) / RobotConstants.LiftMotorTicksPerRotationofOuputShaft;
        SlidePositionIN = Math.PI * 1.5 * bulkData2.getMotorCurrentPosition(SlideEncoder) / 360;

        slideObstruction = LiftPositionIN < 8;
        liftObstruction = Math.abs(SlidePositionIN - slideTargetIN) > (LiftPositionIN < 5 ? 4 : 1);

        if (liftObstruction && liftTargetIN <= 8 && LiftPositionIN < 8.3) {
            double liftOffset = (bulkData2.getMotorCurrentPosition(LeftLift) - bulkData2.getMotorCurrentPosition(RightLift)) / (LeftLift.getMotorType().getTicksPerRev());

            liftPower = 8.4 - LiftPositionIN;
            LeftLift.setPower(liftPower + Math.max(0, -liftOffset));
            RightLift.setPower(liftPower + Math.max(0, liftOffset));

        } else if (liftObstruction && liftTargetIN <= 8 && LiftPositionIN < 11) {
            LeftLift.setPower(0);
            RightLift.setPower(0);
        } else {

            //LeftLift.setTargetPosition(LiftTarget);
            //RightLift.setTargetPosition(LiftTarget);


            double liftOffset = (bulkData2.getMotorCurrentPosition(LeftLift) - bulkData2.getMotorCurrentPosition(RightLift)) / (LeftLift.getMotorType().getTicksPerRev());

//            liftPower = (liftTargetIN - LiftPositionIN);
            liftPower = 0.5 * ((liftTargetIN == 0 ? -0.5 : liftTargetIN) - LiftPositionIN);

            LeftLift.setPower(liftPower + Math.max(0, -liftOffset));
            RightLift.setPower(liftPower + Math.max(0, liftOffset));
        }

        isBusy = Math.abs(LiftPositionIN - liftTargetIN) > 1 || Math.abs(SlidePositionIN - slideTargetIN) > 1;

        if (Math.abs(SlidePositionIN - slideTargetIN) < 0.5) {
            Elbow.setPosition(0.5);
        } else {
            if (slideObstruction)
                Elbow.setPosition(0.5);
            else
                Elbow.setPosition(Range.clip(
                        Range.scale(
                                (slideTargetIN - SlidePositionIN), -1, 1, 0.2, 0.8),
                        0.2, 0.8));
        }

    }

    double liftPgain = 0.5;    //up and down hold tuning, times inches
    double liftOffsetPgain = 0.5;   //left and right tuning, times inches
    double minimumLiftMovementHeight = 6.5;
    double minimumLiftMovementTarget = 8;
    double liftHeightTolerance = 1;

    public void update(RevBulkData bulkData2, double triggerSum, boolean override, boolean slideOverride) {

        LiftPositionIN = Math.PI * 1.25 * Math.max(LeftLift.getCurrentPosition(), RightLift.getCurrentPosition()) / RobotConstants.LiftMotorTicksPerRotationofOuputShaft;
        SlidePositionIN = Math.PI * 1.5 * bulkData2.getMotorCurrentPosition(SlideEncoder) / 360;


        slideObstruction = LiftPositionIN < minimumLiftMovementHeight;
        liftObstruction = Math.abs(SlidePositionIN - slideTargetIN) > (slideObstruction ? 3 : 1);
        double effectiveLiftTarget = liftTargetIN;

        if (effectiveLiftTarget == 0)
            effectiveLiftTarget = -liftHeightTolerance;

        if (liftObstruction)
            effectiveLiftTarget = Math.max(minimumLiftMovementTarget, effectiveLiftTarget);

        if (override) {
            Elbow.setPosition(0.5);
            if (Math.abs(triggerSum) > 0.1) {
                LeftLift.setPower(triggerSum);
                RightLift.setPower(triggerSum);
                liftTargetIN = LiftPositionIN;
            } else {
                RightLift.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                LeftLift.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                start();
            }

        } else {

            liftPower = liftPgain * (effectiveLiftTarget - LiftPositionIN);

            if (Math.abs(effectiveLiftTarget - LiftPositionIN) < liftHeightTolerance) {
                isBusy = false;
                liftPower = 0;
            } else
                isBusy = true;

            if (Math.abs(triggerSum) > 0.1) {
                liftPower = triggerSum;
                liftTargetIN = LiftPositionIN + 0.1 * triggerSum;
            }

            double liftOffsetIN = Math.PI * 1.25 * (bulkData2.getMotorCurrentPosition(LeftLift) - bulkData2.getMotorCurrentPosition(RightLift)) / RobotConstants.LiftMotorTicksPerRotationofOuputShaft;
            double liftOffset = liftOffsetPgain * liftOffsetIN;

            LeftLift.setPower(liftPower + Math.max(0, -liftOffset));
            RightLift.setPower(liftPower + Math.max(0, liftOffset));


            if (slideOverride) {
                slideTargetIN = SlidePositionIN;
            } else {
                if (Math.abs(SlidePositionIN - slideTargetIN) < .15 || slideObstruction) {
                    Elbow.setPosition(0.5);
                } else {
                    Elbow.setPosition(Range.clip(
                            Range.scale(
                                    (slideTargetIN - SlidePositionIN), -1, 1, 0.2, 0.8),
                            0.2, 0.8));
                    isBusy = true;
                }
            }
        }

    }
}