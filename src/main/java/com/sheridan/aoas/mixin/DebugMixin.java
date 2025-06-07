package com.sheridan.aoas.mixin;

import net.irisshaders.iris.pipeline.transform.Patch;
import net.irisshaders.iris.pipeline.transform.PatchShaderType;
import net.irisshaders.iris.pipeline.transform.TransformPatcher;
import net.irisshaders.iris.pipeline.transform.parameter.Parameters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(TransformPatcher.class)
public class DebugMixin {

    @Inject(method = "transform", at = @At("TAIL"), remap = false)
    private static void test1(String name, String vertex, String geometry, String tessControl, String tessEval, String fragment, Parameters parameters, CallbackInfoReturnable<Map<PatchShaderType, String>> cir) {
        if (parameters.patch == Patch.VANILLA) {//shadow_entities_cutout  //entities_cutout_diffuse
            if ("shadow_entities_cutout".equals(name) || "entities_cutout_diffuse".equals(name)) {
                Map<PatchShaderType, String> returnValue = cir.getReturnValue();
                String vsh = returnValue.get(PatchShaderType.VERTEX);
                vsh = injectUniformsAndTransformLogic(vsh);
                returnValue.put(PatchShaderType.VERTEX, vsh);
            }
        }
    }

    private static String injectUniformsAndTransformLogic(String vsh) {

        if (vsh.contains("uniform int doTransformOverride;")) {
            return vsh;
        }

        int versionIndex = vsh.indexOf("#version");
        if (versionIndex == -1) {
            System.err.println("Shader is missing #version directive!");
            return vsh;
        }

        String uniforms = "";
        if (!vsh.contains("uniform int doTransformOverride;")) {
            uniforms += "uniform int doTransformOverride;\n";
        }
        if (!vsh.contains("uniform mat3 myTransformMat3;")) {
            uniforms += "uniform mat3 myTransformMat3;\n";
        }

        if (!vsh.contains("uniform mat4 myTransformMat4;")) {
            uniforms += "uniform mat4 myTransformMat4;\n";
        }

        if (!vsh.contains("vec3 transformedNormal")) {
            uniforms += "vec3 transformedNormal;\n";
        }

        if (!vsh.contains("vec4 transformedPos")) {
            uniforms += "vec4 transformedPos;\n";
        }

        int versionLineEnd = vsh.indexOf('\n', versionIndex);
        String beforeVersionEnd = vsh.substring(0, versionLineEnd + 1);
        String afterVersionEnd = vsh.substring(versionLineEnd + 1);
        String s = beforeVersionEnd + uniforms + afterVersionEnd;

        int mainIndex = s.indexOf("void main()");
        if (mainIndex == -1) return vsh; // 没找到 main，返回原样

        int braceOpen = s.indexOf("{", mainIndex);
        if (braceOpen == -1) return vsh;

        String beforeMainBody = s.substring(0, braceOpen + 1); // 包含 {
        String afterMainBody = s.substring(braceOpen + 1);

        String injectedCode = """
                
                transformedNormal = iris_Normal;
                transformedPos = vec4(iris_Position, 1.0);
                if (doTransformOverride == 1) {
                transformedNormal = myTransformMat3 * iris_Normal;
                transformedPos = myTransformMat4 * transformedPos;
                }
                """;

        String program = beforeMainBody + injectedCode + afterMainBody;
        String[] split = program.split("\n");
        for (int i = 0; i < split.length; i++) {
            if (!(split[i].startsWith("in") || "transformedNormal = myTransformMat3 * iris_Normal;".equals(split[i]) ||
                    "transformedNormal = iris_Normal;".equals(split[i]))) {
                split[i] = split[i].replaceAll("\\biris_Normal\\b(?!\\w)", "transformedNormal");
            }
            if (!("transformedPos = vec4(iris_Position, 1.0);".equals(split[i]))) {
                split[i] = split[i].replaceAll("vec4\\s*\\(\\s*iris_Position\\s*,\\s*1\\.0\\s*f?\\s*\\)", "transformedPos");
            }
        }
        program = String.join("\n", split);
        return program;
    }

}
