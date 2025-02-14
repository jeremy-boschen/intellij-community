// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.lang.ExpressionTypeProvider
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KtAllowAnalysisFromWriteAction
import org.jetbrains.kotlin.analysis.api.KtAllowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.lifetime.allowAnalysisFromWriteAction
import org.jetbrains.kotlin.analysis.api.lifetime.allowAnalysisOnEdt
import org.jetbrains.kotlin.idea.codeinsight.utils.isImplicitInvokeCall
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf

abstract class KotlinExpressionTypeProvider : ExpressionTypeProvider<KtExpression>() {
    protected abstract fun KtExpression.shouldShowStatementType(): Boolean

    override fun getExpressionsAt(elementAt: PsiElement): List<KtExpression> {
        val candidates = elementAt.parentsWithSelf.filterIsInstance<KtExpression>().filter { it.shouldShowType() }.toList()
        val fileEditor =
            elementAt.containingFile?.virtualFile?.let { FileEditorManager.getInstance(elementAt.project).getSelectedEditor(it) }
        val selectionTextRange = if (fileEditor is TextEditor) {
            EditorUtil.getSelectionInAnyMode(fileEditor.editor)
        } else {
            TextRange.EMPTY_RANGE
        }
        val anchor =
            candidates.firstOrNull { selectionTextRange.isEmpty || it.textRange.contains(selectionTextRange) } ?: return emptyList()
        return candidates.filter { it.textRange.startOffset == anchor.textRange.startOffset }
    }

    private fun KtExpression.shouldShowType() = when (this) {
        is KtFunctionLiteral -> false
        is KtFunction -> !hasBlockBody() && !hasDeclaredReturnType()
        is KtProperty -> typeReference == null
        is KtParameter -> typeReference == null && (isLoopParameter || isLambdaParameter)
        is KtPropertyAccessor -> false
        is KtDestructuringDeclarationEntry -> true
        is KtStatementExpression, is KtDestructuringDeclaration -> false
        is KtIfExpression, is KtWhenExpression, is KtTryExpression -> shouldShowStatementType()
        is KtConstantExpression -> false
        is KtThisExpression -> false
        else -> getQualifiedExpressionForSelector() == null && parent !is KtCallableReferenceExpression && !isFunctionCallee()
    }

    @OptIn(KtAllowAnalysisOnEdt::class, KtAllowAnalysisFromWriteAction::class)
    private fun KtExpression.isFunctionCallee(): Boolean {
        val callExpression = parent as? KtCallExpression ?: return false
        if (callExpression.calleeExpression != this) return false

        // getExpressionsAt is executed from EDT
        allowAnalysisOnEdt {
            allowAnalysisFromWriteAction {
                analyze(callExpression) {
                    return callExpression.isImplicitInvokeCall() == false
                }
            }
        }
    }
}
