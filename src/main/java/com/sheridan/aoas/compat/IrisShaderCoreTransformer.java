package com.sheridan.aoas.compat;

import io.github.douira.glsl_transformer.ast.node.TranslationUnit;
import io.github.douira.glsl_transformer.ast.query.Root;
import io.github.douira.glsl_transformer.ast.transform.ASTParser;
import io.github.douira.glsl_transformer.util.Type;
import io.github.douira.glsl_transformer.ast.node.type.qualifier.StorageQualifier.StorageType;
import net.irisshaders.iris.pipeline.transform.parameter.VanillaParameters;
import net.irisshaders.iris.pipeline.transform.transformer.CommonTransformer;

public class IrisShaderCoreTransformer {

    public static void transform(ASTParser t, TranslationUnit tree, Root root, VanillaParameters parameters) {
        // 仅处理 vertex shader
        if (parameters.type != net.irisshaders.iris.pipeline.transform.PatchShaderType.VERTEX) return;
        // 注入 uniform
        CommonTransformer.addIfNotExists(root, t, tree, "doTransformOverride", Type.BOOL, StorageType.UNIFORM);
        CommonTransformer.addIfNotExists(root, t, tree, "aoas_TransformMat4", Type.F32MAT4X4, StorageType.UNIFORM);
        CommonTransformer.addIfNotExists(root, t, tree, "aoas_TransformMat3", Type.F32MAT3X3, StorageType.UNIFORM);


//        FunctionDefinition mainDef = root.identifierIndex.getOne("main");
//        if (mainDef == null) return;
//
//        // 获取main函数体
//        CompoundStatement body = (CompoundStatement) mainDef.getBody();
//        if (body == null) return;
//
//        // 构建条件语句代码
//        String conditionCode =
//                "if (doTransformOverride) {\n" +
//                        "    gl_Position = myTransformMat4 * gl_Position;\n" +
//                        "    gl_Normal = myTransformMat3 * gl_Normal;\n" +
//                        "}";

        // 将条件语句注入到main函数体开头
//        Expression conditionStmt = t.parseStatement(conditionCode);
//        body.inject(t, ASTInjectionPoint.BEFORE_FIRST_STATEMENT, conditionStmt);
        //TODO: 注入条件语句到main开头
//        "if (doTransformOverride) {",
//                "    gl_Position = myTransformMat4 * gl_Position;",
//                "    gl_Normal = myTransformMat3 * gl_Normal;",
//                "}"

    }
}
