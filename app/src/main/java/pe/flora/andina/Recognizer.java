package pe.flora.andina;

import java.util.List;

final class Recognizer {

    record Match(
            String scientific,
            String common,
            float score
    ) {}

    record Result(
            List<Match> matches,
            String engine
    ) {}

}
