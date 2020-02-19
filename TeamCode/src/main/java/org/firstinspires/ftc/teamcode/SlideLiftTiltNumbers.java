public class SlideLiftTiltNumbers {

    public static double scale(double n, double x1, double x2, double y1, double y2) {
        double a = (y1 - y2) / (x1 - x2);
        double b = y1 - x1 * (y1 - y2) / (x1 - x2);
        return a * n + b;
    }

    public static double getTheThing(double input) {
        input *= 1.0;
        final double[] inputs = {0, 14.404, 17.8, 21.439, 25.57, 28.9, 32.4, 35.9, 38.9, 41.9};
        final double[] outputs = {16, 16.768, 16.8467, 17.0955, 17.2656, 16.87, 17.318, 17.488, 18.064, 17.462};

        if (inputs.length != outputs.length)
            try {
                throw new Exception("the number of inputs and outputs for the slide/lift tilt thing need to match");
            } catch (Exception e) {
                e.printStackTrace();
            }

        if (input <= inputs[0])
            return outputs[0];
        else if (input > inputs[inputs.length - 1])
            return outputs[outputs.length - 1];

        int index = 0;
        for (; index < inputs.length; index++)
            if (input < inputs[index])
                break;

        return scale(input, inputs[index - 1], inputs[index], outputs[index - 1], outputs[index]);
    }
}
