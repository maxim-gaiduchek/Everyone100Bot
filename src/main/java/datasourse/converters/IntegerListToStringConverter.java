package datasourse.converters;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.ArrayList;
import java.util.List;

@Converter
public class IntegerListToStringConverter implements AttributeConverter<List<Integer>, String> {

    private static final String SPLIT_REGEX = ",";

    @Override
    public String convertToDatabaseColumn(List<Integer> longs) {
        if (longs == null || longs.isEmpty()) return "";

        StringBuilder sb = new StringBuilder().append(longs.get(0));
        for (int i = 1; i < longs.size(); i++) sb.append(SPLIT_REGEX).append(longs.get(i));

        return sb.toString();
    }

    @Override
    public List<Integer> convertToEntityAttribute(String s) {
        List<Integer> ids = new ArrayList<>();

        s = s.trim();
        if (!s.equals("")) {
            for (String id : s.split(SPLIT_REGEX)) {
                ids.add(Integer.parseInt(id));
            }
        }

        return ids;
    }
}
