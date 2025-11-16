package com.intellij.plugin.applescript.lang.ide.annotator;

import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugin.applescript.lang.ide.highlighting.AppleScriptSyntaxHighlighterColors;
import com.intellij.plugin.applescript.lang.ide.intentions.AddApplicationDictionaryQuickFix;
import com.intellij.plugin.applescript.lang.ide.intentions.RenameParameterLabelQuickFix;
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptProjectDictionaryService;
import com.intellij.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistryService;
import com.intellij.plugin.applescript.lang.sdef.ApplicationDictionary;
import com.intellij.plugin.applescript.psi.*;
import com.intellij.plugin.applescript.psi.impl.AppleScriptPsiImplUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

import static com.intellij.plugin.applescript.psi.AppleScriptTokenTypesSets.HANDLER_PARAMETER_LABELS;
import static com.intellij.plugin.applescript.psi.AppleScriptTypes.*;

/**
 * Andrey 12.04.2015
 */
public class AppleScriptColorAnnotator implements Annotator {
  private static void createInfoAnnotation(final @NotNull AnnotationHolder holder,
                                           final @Nullable PsiElement element,
                                           final @Nullable TextAttributesKey attributeKey) {
    if (element != null && attributeKey != null) {
      holder.newAnnotation(com.intellij.lang.annotation.HighlightSeverity.INFORMATION, "")
          .range(element)
          .textAttributes(attributeKey)
          .create();
    }
  }

  @Override
  public void annotate(final @NotNull PsiElement element, final @NotNull AnnotationHolder holder) {
    IElementType elementType = element.getNode().getElementType();
    // Don't check dictionary elements for unresolved references at all
    // This prevents false positives when application dictionaries are not loaded
    if (element instanceof AppleScriptHandlerCall) {
      PsiElement resolveResult = null;
      PsiReference ref = element.getReference();
      if (ref != null) {
        resolveResult = element.getReference().resolve();
      }
      if (resolveResult == null) {
        AppleScriptHandlerCall handlerCall = (AppleScriptHandlerCall) element;
        for (AppleScriptHandlerArgument argument : handlerCall.getArguments()) {
          createInfoAnnotation(holder, argument.getArgumentSelector(), AppleScriptSyntaxHighlighterColors.UNRESOLVED_REFERENCE);
        }
      }
    }
    if (element instanceof AppleScriptDictionaryCommandName) {
      createInfoAnnotation(holder, element, AppleScriptSyntaxHighlighterColors.DICTIONARY_COMMAND_ATTR);
    } else if (element instanceof AppleScriptCommandParameterSelector) {
      createInfoAnnotation(holder, element, AppleScriptSyntaxHighlighterColors.DICTIONARY_COMMAND_SELECTOR_ATTR);
    } else if (element instanceof AppleScriptDictionaryClassName || element instanceof AppleScriptDictionaryClassIdentifierPlural || element instanceof AppleScriptBuiltInClassIdentifier) {
      createInfoAnnotation(holder, element, AppleScriptSyntaxHighlighterColors.DICTIONARY_CLASS_ATTR);
    } else if (element instanceof AppleScriptDictionaryPropertyName) {
      createInfoAnnotation(holder, element, AppleScriptSyntaxHighlighterColors.DICTIONARY_PROPERTY_ATTR);
    } else if (element instanceof AppleScriptDictionaryConstant) {
      createInfoAnnotation(holder, element, AppleScriptSyntaxHighlighterColors.DICTIONARY_CONSTANT_ATTR);
    } else if (element instanceof AppleScriptApplicationReference) {
      AppleScriptApplicationReference appRef = (AppleScriptApplicationReference) element;
      annotateApplicationReference(holder, appRef, false);
    } else if (element instanceof AppleScriptIncompleteExpression) {
      AppleScriptExpression expression = PsiTreeUtil.findChildOfType(element, AppleScriptExpression.class);
      AppleScriptApplicationReference appRef = PsiTreeUtil.findChildOfType(expression, AppleScriptApplicationReference.class);
      if (appRef != null) {
        annotateApplicationReference(holder, appRef, true);
      }
      holder.newAnnotation(com.intellij.lang.annotation.HighlightSeverity.ERROR, "Incomplete expression")
          .range(element)
          .create();
    }
    // duplicated labels
    if (elementType == HANDLER_LABELED_PARAMETERS_DEFINITION ||
        elementType == HANDLER_LABELED_PARAMETERS_CALL_EXPRESSION) {
      PsiElement[] childElements = element.getChildren();
      ArrayList<String> labelNames = new ArrayList<>();
      for (PsiElement childElement : childElements) {
        if (childElement.getNode().getElementType() == HANDLER_PARAMETER_LABEL) {
          String labelName = childElement.getText();
          if (labelNames.contains(labelName)) {
            String newLabelName = null;
            for (IElementType type : HANDLER_PARAMETER_LABELS.getTypes()) {
              if (!labelNames.contains(type.toString().toLowerCase())) {
                newLabelName = type.toString().toLowerCase().replaceAll("_", " ");
              }
            }
            holder.newAnnotation(com.intellij.lang.annotation.HighlightSeverity.ERROR, "Duplicated parameter label '" + labelName + "'")
                .range(childElement)
                .withFix(new RenameParameterLabelQuickFix((AppleScriptHandlerParameterLabel) childElement, newLabelName))
                .create();
          }
          labelNames.add(labelName);
        }
      }
    }

  }

  private void annotateApplicationReference(@NotNull AnnotationHolder holder, @NotNull AppleScriptApplicationReference appRef, boolean error) {
    String appName = AppleScriptPsiImplUtil.getNameFromApplicationReference(appRef);
    AppleScriptSystemDictionaryRegistryService dictionaryRegistryService =
        AppleScriptSystemDictionaryRegistryService.getInstance();

    if (dictionaryRegistryService == null || StringUtil.isEmptyOrSpaces(appName)) return;

    if (!dictionaryRegistryService.isDictionaryInitialized(appName)) {
      String warningReason = checkWarningReason(appName, dictionaryRegistryService);
      if (warningReason == null && !dictionaryRegistryService.ensureDictionaryInitialized(appName)) {
        System.out.println("Re-checking warning reason for " + appName);
        warningReason = checkWarningReason(appName, dictionaryRegistryService);
      }
      if (!StringUtil.isEmpty(warningReason)) {
        if (error) {
          holder.newAnnotation(com.intellij.lang.annotation.HighlightSeverity.ERROR, warningReason)
              .range(appRef)
              .textAttributes(CodeInsightColors.WARNINGS_ATTRIBUTES)
              .withFix(new AddApplicationDictionaryQuickFix(appName))
              .create();
        } else {
          holder.newAnnotation(com.intellij.lang.annotation.HighlightSeverity.WARNING, warningReason)
              .range(appRef)
              .withFix(new AddApplicationDictionaryQuickFix(appName))
              .create();
        }
      } else {
        AppleScriptProjectDictionaryService dictionaryProjectService =
            appRef.getProject().getService(AppleScriptProjectDictionaryService.class);
        ApplicationDictionary dictionary = dictionaryProjectService.getDictionary(appName);
        if (dictionary == null) dictionary = dictionaryProjectService.createDictionary(appName);
        if (dictionary == null) {
          if (error) {
            holder.newAnnotation(com.intellij.lang.annotation.HighlightSeverity.ERROR, "Unknown app \"" + appName + "\"?")
                .range(appRef)
                .textAttributes(CodeInsightColors.WARNINGS_ATTRIBUTES)
                .withFix(new AddApplicationDictionaryQuickFix(appName))
                .create();
          } else {
            holder.newAnnotation(com.intellij.lang.annotation.HighlightSeverity.WARNING, "Unknown app \"" + appName + "\"?")
                .range(appRef)
                .create();
          }
        }
      }
    } else {
      AppleScriptProjectDictionaryService dictionaryProjectService = 
          appRef.getProject().getService(AppleScriptProjectDictionaryService.class);
      ApplicationDictionary dictionary = dictionaryProjectService.getDictionary(appName);
      if (dictionary == null) dictionaryProjectService.createDictionary(appName);
    }
  }

  @Nullable
  private String checkWarningReason(@NotNull String appName, @NotNull AppleScriptSystemDictionaryRegistryService dictionaryRegistryService) {
    String warningReason = null;
    if (dictionaryRegistryService.isNotScriptable(appName) && dictionaryRegistryService.isXcodeInstalled()) {
      warningReason = "Application \"" + appName + "\" is not scriptable";
    } else if (dictionaryRegistryService.isInUnknownList(appName)) {
      warningReason = "Application \"" + appName + "\" not found";
    } else if (!dictionaryRegistryService.isXcodeInstalled()) {
      warningReason = "Can not create dictionary: Xcode Developer Tools are not installed";
    }
    return warningReason;
  }
}
