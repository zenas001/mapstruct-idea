/*
 *  Copyright 2017 the MapStruct authors (http://www.mapstruct.org/)
 *  and/or other contributors as indicated by the @authors tag. See the
 *  copyright.txt file in the distribution for a full listing of all
 *  contributors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.mapstruct.intellij.inspection;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.daemon.impl.quickfix.RemoveUnusedVariableFix;
import com.intellij.codeInsight.intention.AddAnnotationPsiFix;
import com.intellij.codeInsight.intention.QuickFixFactory;
import com.intellij.codeInspection.IntentionWrapper;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiVariable;
import com.siyeh.ig.callMatcher.CallMatcher;
import com.siyeh.ig.psiutils.ParenthesesUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.mapstruct.intellij.MapStructBundle;
import org.mapstruct.intellij.util.MapstructUtil;

/**
 * Inspection that checks that Mappers factory is correctly used
 *
 * @author Filip Hrisafov
 */
public class WrongUsageOfMappersFactoryInspection extends InspectionBase {

    private static final CallMatcher MAPPERS_FACTORY_CALL_MATCHER = CallMatcher.staticCall(
        MapstructUtil.MAPPERS_FQN,
        "getMapper"
    ).parameterTypes( CommonClassNames.JAVA_LANG_CLASS );

    @NotNull
    @Override
    PsiElementVisitor buildVisitorInternal(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new MyJavaElementVisitor( holder );
    }

    private static class MyJavaElementVisitor extends JavaElementVisitor {
        private final ProblemsHolder problemsHolder;

        private MyJavaElementVisitor(ProblemsHolder problemsHolder) {
            this.problemsHolder = problemsHolder;
        }

        @Override
        public void visitMethodCallExpression(PsiMethodCallExpression expression) {
            if ( MAPPERS_FACTORY_CALL_MATCHER.test( expression ) ) {
                PsiExpression argument = ParenthesesUtils.stripParentheses( expression.getArgumentList()
                    .getExpressions()[0] );
                if ( !( argument instanceof PsiClassObjectAccessExpression ) ) {
                    return;
                }
                PsiClassObjectAccessExpression classObjectAccessExpression = (PsiClassObjectAccessExpression) argument;
                PsiJavaCodeReferenceElement referenceElement = classObjectAccessExpression.getOperand()
                    .getInnermostComponentReferenceElement();

                if ( referenceElement == null ) {
                    return;
                }

                PsiElement mapperElement = referenceElement.resolve();

                if ( !( mapperElement instanceof PsiClass ) ) {
                    return;
                }

                PsiClass mapperClass = (PsiClass) mapperElement;
                PsiAnnotation mapperAnnotation = mapperClass.getAnnotation( MapstructUtil.MAPPER_ANNOTATION_FQN );
                if ( mapperAnnotation == null ) {
                    problemsHolder.registerProblem(
                        expression,
                        MapStructBundle.message( "inspection.wrong.usage.mappers.factory.non.mapstruct" ),
                        new AddAnnotationPsiFix(
                            MapstructUtil.MAPPER_ANNOTATION_FQN,
                            mapperClass,
                            PsiNameValuePair.EMPTY_ARRAY
                        ),
                        createRemoveMappersFix( expression )
                    );
                }
                else {
                    PsiNameValuePair componentModelAttribute = AnnotationUtil.findDeclaredAttribute(
                        mapperAnnotation,
                        "componentModel"
                    );
                    PsiAnnotationMemberValue memberValue = componentModelAttribute == null ?
                        null :
                        componentModelAttribute.getDetachedValue();
                    String componentModel = memberValue == null ?
                        null :
                        AnnotationUtil.getStringAttributeValue( memberValue );
                    if ( componentModel != null && !componentModel.equals( "default" ) ) {
                        problemsHolder.registerProblem(
                            expression,
                            MapStructBundle.message( "inspection.wrong.usage.mappers.factory.non.default" ),
                            createRemoveComponentModelFix( componentModelAttribute, mapperClass ),
                            createRemoveMappersFix( expression )
                        );
                    }
                }
            }
        }
    }

    private static class RemoveMappersFix extends RemoveUnusedVariableFix {

        private final String myText;
        private final String myFamilyName;

        private RemoveMappersFix(@NotNull PsiVariable element) {
            super( element );
            this.myFamilyName = MapStructBundle.message( "inspection.wrong.usage.mappers.factory" );
            this.myText = MapStructBundle.message( "inspection.wrong.usage.mappers.factory.remove.mappers.usage" );
        }

        @NotNull
        @Override
        public String getText() {
            return myText;
        }

        @Nls(capitalization = Nls.Capitalization.Sentence)
        @NotNull
        @Override
        public String getFamilyName() {
            return myFamilyName;
        }
    }

    private static LocalQuickFix createRemoveMappersFix(@NotNull PsiMethodCallExpression methodCallExpression) {
        PsiElement parent = methodCallExpression.getParent();
        if ( parent instanceof PsiVariable ) {
            return IntentionWrapper.wrapToQuickFix(
                new RemoveMappersFix( (PsiVariable) parent ),
                methodCallExpression.getContainingFile()
            );
        }

        return null;
    }

    private static LocalQuickFix createRemoveComponentModelFix(@NotNull PsiNameValuePair componentModelPair,
        @NotNull PsiClass psiClass) {
        return QuickFixFactory.getInstance().createDeleteFix(
            componentModelPair,
            MapStructBundle.message(
                "inspection.wrong.usage.mappers.factory.remove.component.model",
                componentModelPair.getLiteralValue(),
                psiClass.getName()
            )
        );
    }

}
