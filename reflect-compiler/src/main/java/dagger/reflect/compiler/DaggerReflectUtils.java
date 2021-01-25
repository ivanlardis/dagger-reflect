package dagger.reflect.compiler;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

public class DaggerReflectUtils {

    public static String toLowerCaseFirstLetter(String name) {
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    public static @Nullable AnnotationMirror getAnnotationMirror(TypeElement typeElement,
                                                                 Class<?> clazz) {
        String clazzName = clazz.getName();
        for (AnnotationMirror m : typeElement.getAnnotationMirrors()) {
            if (m.getAnnotationType().toString().equals(clazzName)) {
                return m;
            }
        }
        return null;
    }

    public static @Nullable AnnotationValue getAnnotationValue(AnnotationMirror annotationMirror,
                                                               String key) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry
                : annotationMirror.getElementValues().entrySet()) {
            if (entry.getKey().getSimpleName().toString().equals(key)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
