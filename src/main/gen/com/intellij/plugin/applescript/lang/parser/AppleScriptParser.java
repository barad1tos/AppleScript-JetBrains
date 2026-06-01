// This is a generated file. Not intended for manual editing.
package com.intellij.plugin.applescript.lang.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static com.intellij.plugin.applescript.psi.AppleScriptTypes.*;
import static com.intellij.plugin.applescript.lang.parser.AppleScriptGeneratedParserUtil.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class AppleScriptParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType root_, PsiBuilder builder_) {
    parseLight(root_, builder_);
    return builder_.getTreeBuilt();
  }

  public void parseLight(IElementType root_, PsiBuilder builder_) {
    boolean result_;
    builder_ = adapt_builder_(root_, builder_, this, EXTENDS_SETS_);
    Marker marker_ = enter_section_(builder_, 0, _COLLAPSE_, null);
    result_ = parse_root_(root_, builder_);
    exit_section_(builder_, 0, marker_, root_, result_, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType root_, PsiBuilder builder_) {
    return parse_root_(root_, builder_, 0);
  }

  static boolean parse_root_(IElementType root_, PsiBuilder builder_, int level_) {
    return appleScriptFile(builder_, level_ + 1);
  }

  public static final TokenSet[] EXTENDS_SETS_ = new TokenSet[] {
    create_token_set_(ACTIVATE_COMMAND_EXPRESSION, ADDITIVE_EXPRESSION, A_REFERENCE_TO_LITERAL_EXPRESSION, BUILT_IN_CONSTANT_LITERAL_EXPRESSION,
      COERCION_EXPRESSION, COMPARE_EXPRESSION, CONCATENATION_EXPRESSION, COUNT_COMMAND_EXPRESSION,
      DATE_LITERAL_EXPRESSION, DICTIONARY_COMMAND_HANDLER_CALL_EXPRESSION, ERROR_COMMAND_EXPRESSION, EXPRESSION,
      GET_COMMAND_EXPRESSION, GIVEN_RAW_PARAMETER_EXPRESSION, HANDLER_LABELED_PARAMETERS_CALL_EXPRESSION, HANDLER_POSITIONAL_PARAMETERS_CALL_EXPRESSION,
      INCOMPLETE_EXPRESSION, INTEGER_LITERAL_EXPRESSION, LAUNCH_COMMAND_EXPRESSION, LIST_LITERAL_EXPRESSION,
      LOGICAL_AND_EXPRESSION, LOGICAL_OR_EXPRESSION, LOG_COMMAND_EXPRESSION, MULTIPLICATIVE_EXPRESSION,
      NEGATION_EXPRESSION, NUMBER_LITERAL_EXPRESSION, OBJECT_REFERENCE_EXPRESSION, PARENTHESIZED_EXPRESSION,
      POWER_EXPRESSION, RAW_CLASS_EXPRESSION, RAW_DATA_EXPRESSION, RAW_DICTIONARY_COMMAND_HANDLER_CALL_EXPRESSION,
      RAW_PARAMETER_EXPRESSION, REAL_LITERAL_EXPRESSION, RECORD_LITERAL_EXPRESSION, REFERENCE_EXPRESSION,
      RUN_COMMAND_EXPRESSION, STRING_LITERAL_EXPRESSION),
  };

  /* ********************************************************** */
  // CENTIMETRES|CENTIMETERS|FEET|INCHES|KILOMETRES|KILOMETERS|METRES|METERS|MILES|YARDS
  static boolean Length(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "Length")) return false;
    boolean result_;
    result_ = consumeToken(builder_, CENTIMETRES);
    if (!result_) result_ = consumeToken(builder_, CENTIMETERS);
    if (!result_) result_ = consumeToken(builder_, FEET);
    if (!result_) result_ = consumeToken(builder_, INCHES);
    if (!result_) result_ = consumeToken(builder_, KILOMETRES);
    if (!result_) result_ = consumeToken(builder_, KILOMETERS);
    if (!result_) result_ = consumeToken(builder_, METRES);
    if (!result_) result_ = consumeToken(builder_, METERS);
    if (!result_) result_ = consumeToken(builder_, MILES);
    if (!result_) result_ = consumeToken(builder_, YARDS);
    return result_;
  }

  /* ********************************************************** */
  // GALLONS|LITRES|LITERS|QUARTS
  static boolean LiquidVolume(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "LiquidVolume")) return false;
    boolean result_;
    result_ = consumeToken(builder_, GALLONS);
    if (!result_) result_ = consumeToken(builder_, LITRES);
    if (!result_) result_ = consumeToken(builder_, LITERS);
    if (!result_) result_ = consumeToken(builder_, QUARTS);
    return result_;
  }

  /* ********************************************************** */
  // GRAMS|KILOGRAMS|OUNCES|POUNDS
  static boolean Weight(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "Weight")) return false;
    boolean result_;
    result_ = consumeToken(builder_, GRAMS);
    if (!result_) result_ = consumeToken(builder_, KILOGRAMS);
    if (!result_) result_ = consumeToken(builder_, OUNCES);
    if (!result_) result_ = consumeToken(builder_, POUNDS);
    return result_;
  }

  /* ********************************************************** */
  // REF_OP objectReferenceWrapper
  public static boolean aReferenceToLiteralExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "aReferenceToLiteralExpression")) return false;
    if (!nextTokenIsFast(builder_, REF_OP)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, REF_OP);
    result_ = result_ && objectReferenceWrapper(builder_, level_ + 1);
    exit_section_(builder_, marker_, A_REFERENCE_TO_LITERAL_EXPRESSION, result_);
    return result_;
  }

  /* ********************************************************** */
  // activate expression?
  public static boolean activateCommandExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "activateCommandExpression")) return false;
    if (!nextTokenIsFast(builder_, ACTIVATE)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, ACTIVATE);
    result_ = result_ && activateCommandExpression_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, ACTIVATE_COMMAND_EXPRESSION, result_);
    return result_;
  }

  // expression?
  private static boolean activateCommandExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "activateCommandExpression_1")) return false;
    expression(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // (PLUS|MINUS) multiplicativeExpressionWrapper
  public static boolean additiveExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "additiveExpression")) return false;
    if (!nextTokenIsFast(builder_, MINUS, PLUS)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, ADDITIVE_EXPRESSION, "<additive expression>");
    result_ = additiveExpression_0(builder_, level_ + 1);
    result_ = result_ && multiplicativeExpressionWrapper(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // PLUS|MINUS
  private static boolean additiveExpression_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "additiveExpression_0")) return false;
    boolean result_;
    result_ = consumeTokenFast(builder_, PLUS);
    if (!result_) result_ = consumeTokenFast(builder_, MINUS);
    return result_;
  }

  /* ********************************************************** */
  // multiplicativeExpressionWrapper additiveExpression*
  static boolean additiveExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "additiveExpressionWrapper")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = multiplicativeExpressionWrapper(builder_, level_ + 1);
    result_ = result_ && additiveExpressionWrapper_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // additiveExpression*
  private static boolean additiveExpressionWrapper_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "additiveExpressionWrapper_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!additiveExpression(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "additiveExpressionWrapper_1", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // anything
  static boolean anythingProperty(PsiBuilder builder_, int level_) {
    return consumeToken(builder_, ANYTHING);
  }

  /* ********************************************************** */
  // getCommandExpression|runCommandExpression|countCommandExpression|
  // errorCommandExpression|logCommandExpression|activateCommandExpression|launchCommandExpression
  static boolean appleScriptCommandExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "appleScriptCommandExpression")) return false;
    boolean result_;
    result_ = getCommandExpression(builder_, level_ + 1);
    if (!result_) result_ = runCommandExpression(builder_, level_ + 1);
    if (!result_) result_ = countCommandExpression(builder_, level_ + 1);
    if (!result_) result_ = errorCommandExpression(builder_, level_ + 1);
    if (!result_) result_ = logCommandExpression(builder_, level_ + 1);
    if (!result_) result_ = activateCommandExpression(builder_, level_ + 1);
    if (!result_) result_ = launchCommandExpression(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // compilation_unit_*
  static boolean appleScriptFile(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "appleScriptFile")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!compilation_unit_(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "appleScriptFile", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // BUILT_IN_PROPERTY|resultProperty|versionProperty|anythingProperty
  // |textItemDelimitersProperty|parentProperty
  public static boolean appleScriptProperty(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "appleScriptProperty")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, APPLE_SCRIPT_PROPERTY, "<apple script property>");
    result_ = consumeToken(builder_, BUILT_IN_PROPERTY);
    if (!result_) result_ = resultProperty(builder_, level_ + 1);
    if (!result_) result_ = versionProperty(builder_, level_ + 1);
    if (!result_) result_ = anythingProperty(builder_, level_ + 1);
    if (!result_) result_ = textItemDelimitersProperty(builder_, level_ + 1);
    if (!result_) result_ = parentProperty(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // STRING_LITERAL
  static boolean appleTalkZoneName(PsiBuilder builder_, int level_) {
    return consumeToken(builder_, STRING_LITERAL);
  }

  /* ********************************************************** */
  // (on|to) applicationHandlerDefinitionSignature
  //                                          [given THE_KW? objectTargetPropertyDeclaration (COMMA THE_KW? objectTargetPropertyDeclaration)*] //nls //should be sep here
  //                                          [varDeclarationList] sep
  //                                            blockBody?
  //                                   end [applicationHandlerDefinitionSignature]
  public static boolean applicationHandlerDefinition(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "applicationHandlerDefinition")) return false;
    if (!nextTokenIs(builder_, "<application handler definition>", ON, TO)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, APPLICATION_HANDLER_DEFINITION, "<application handler definition>");
    result_ = applicationHandlerDefinition_0(builder_, level_ + 1);
    result_ = result_ && applicationHandlerDefinitionSignature(builder_, level_ + 1);
    result_ = result_ && applicationHandlerDefinition_2(builder_, level_ + 1);
    result_ = result_ && applicationHandlerDefinition_3(builder_, level_ + 1);
    result_ = result_ && sep(builder_, level_ + 1);
    result_ = result_ && applicationHandlerDefinition_5(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, END);
    result_ = result_ && applicationHandlerDefinition_7(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // on|to
  private static boolean applicationHandlerDefinition_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "applicationHandlerDefinition_0")) return false;
    boolean result_;
    result_ = consumeToken(builder_, ON);
    if (!result_) result_ = consumeToken(builder_, TO);
    return result_;
  }

  // [given THE_KW? objectTargetPropertyDeclaration (COMMA THE_KW? objectTargetPropertyDeclaration)*]
  private static boolean applicationHandlerDefinition_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "applicationHandlerDefinition_2")) return false;
    applicationHandlerDefinition_2_0(builder_, level_ + 1);
    return true;
  }

  // given THE_KW? objectTargetPropertyDeclaration (COMMA THE_KW? objectTargetPropertyDeclaration)*
  private static boolean applicationHandlerDefinition_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "applicationHandlerDefinition_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, GIVEN);
    result_ = result_ && applicationHandlerDefinition_2_0_1(builder_, level_ + 1);
    result_ = result_ && objectTargetPropertyDeclaration(builder_, level_ + 1);
    result_ = result_ && applicationHandlerDefinition_2_0_3(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // THE_KW?
  private static boolean applicationHandlerDefinition_2_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "applicationHandlerDefinition_2_0_1")) return false;
    consumeToken(builder_, THE_KW);
    return true;
  }

  // (COMMA THE_KW? objectTargetPropertyDeclaration)*
  private static boolean applicationHandlerDefinition_2_0_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "applicationHandlerDefinition_2_0_3")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!applicationHandlerDefinition_2_0_3_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "applicationHandlerDefinition_2_0_3", pos_)) break;
    }
    return true;
  }

  // COMMA THE_KW? objectTargetPropertyDeclaration
  private static boolean applicationHandlerDefinition_2_0_3_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "applicationHandlerDefinition_2_0_3_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, COMMA);
    result_ = result_ && applicationHandlerDefinition_2_0_3_0_1(builder_, level_ + 1);
    result_ = result_ && objectTargetPropertyDeclaration(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // THE_KW?
  private static boolean applicationHandlerDefinition_2_0_3_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "applicationHandlerDefinition_2_0_3_0_1")) return false;
    consumeToken(builder_, THE_KW);
    return true;
  }

  // [varDeclarationList]
  private static boolean applicationHandlerDefinition_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "applicationHandlerDefinition_3")) return false;
    varDeclarationList(builder_, level_ + 1);
    return true;
  }

  // blockBody?
  private static boolean applicationHandlerDefinition_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "applicationHandlerDefinition_5")) return false;
    blockBody(builder_, level_ + 1);
    return true;
  }

  // [applicationHandlerDefinitionSignature]
  private static boolean applicationHandlerDefinition_7(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "applicationHandlerDefinition_7")) return false;
    applicationHandlerDefinitionSignature(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // <<parseApplicationHandlerDefinitionSignature>>
  static boolean applicationHandlerDefinitionSignature(PsiBuilder builder_, int level_) {
    return parseApplicationHandlerDefinitionSignature(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // <<parseApplicationName tellStatementStartCondition>>
  static boolean applicationName(PsiBuilder builder_, int level_) {
    return parseApplicationName(builder_, level_ + 1, AppleScriptParser::tellStatementStartCondition);
  }

  /* ********************************************************** */
  // applicationName [of THE_KW? machine machineName (of THE_KW? zone appleTalkZoneName)?] | THE_KW? currentApplicationConstant
  public static boolean applicationReference(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "applicationReference")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, APPLICATION_REFERENCE, "<application reference>");
    result_ = applicationReference_0(builder_, level_ + 1);
    if (!result_) result_ = applicationReference_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // applicationName [of THE_KW? machine machineName (of THE_KW? zone appleTalkZoneName)?]
  private static boolean applicationReference_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "applicationReference_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = applicationName(builder_, level_ + 1);
    result_ = result_ && applicationReference_0_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // [of THE_KW? machine machineName (of THE_KW? zone appleTalkZoneName)?]
  private static boolean applicationReference_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "applicationReference_0_1")) return false;
    applicationReference_0_1_0(builder_, level_ + 1);
    return true;
  }

  // of THE_KW? machine machineName (of THE_KW? zone appleTalkZoneName)?
  private static boolean applicationReference_0_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "applicationReference_0_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, OF);
    result_ = result_ && applicationReference_0_1_0_1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, MACHINE);
    result_ = result_ && machineName(builder_, level_ + 1);
    result_ = result_ && applicationReference_0_1_0_4(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // THE_KW?
  private static boolean applicationReference_0_1_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "applicationReference_0_1_0_1")) return false;
    consumeToken(builder_, THE_KW);
    return true;
  }

  // (of THE_KW? zone appleTalkZoneName)?
  private static boolean applicationReference_0_1_0_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "applicationReference_0_1_0_4")) return false;
    applicationReference_0_1_0_4_0(builder_, level_ + 1);
    return true;
  }

  // of THE_KW? zone appleTalkZoneName
  private static boolean applicationReference_0_1_0_4_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "applicationReference_0_1_0_4_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, OF);
    result_ = result_ && applicationReference_0_1_0_4_0_1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, ZONE);
    result_ = result_ && appleTalkZoneName(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // THE_KW?
  private static boolean applicationReference_0_1_0_4_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "applicationReference_0_1_0_4_0_1")) return false;
    consumeToken(builder_, THE_KW);
    return true;
  }

  // THE_KW? currentApplicationConstant
  private static boolean applicationReference_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "applicationReference_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = applicationReference_1_0(builder_, level_ + 1);
    result_ = result_ && currentApplicationConstant(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // THE_KW?
  private static boolean applicationReference_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "applicationReference_1_0")) return false;
    consumeToken(builder_, THE_KW);
    return true;
  }

  /* ********************************************************** */
  // some typeSpecifier
  public static boolean arbitraryReference(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "arbitraryReference")) return false;
    if (!nextTokenIs(builder_, SOME)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, ARBITRARY_REFERENCE, null);
    result_ = consumeToken(builder_, SOME);
    pinned_ = result_; // pin = 1
    result_ = result_ && typeSpecifier(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // !(RPAREN|COMMA)
  static boolean argListPartRecover(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "argListPartRecover")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_);
    result_ = !argListPartRecover_0(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // RPAREN|COMMA
  private static boolean argListPartRecover_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "argListPartRecover_0")) return false;
    boolean result_;
    result_ = consumeTokenFast(builder_, RPAREN);
    if (!result_) result_ = consumeTokenFast(builder_, COMMA);
    return result_;
  }

  /* ********************************************************** */
  // !(RPAREN)
  static boolean argListRecover(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "argListRecover")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_);
    result_ = !argListRecover_0(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // (RPAREN)
  private static boolean argListRecover_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "argListRecover_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, RPAREN);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // argumentListPart (COMMA argumentListPart)*
  static boolean argumentList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "argumentList")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_);
    result_ = argumentListPart(builder_, level_ + 1);
    result_ = result_ && argumentList_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, AppleScriptParser::argListRecover);
    return result_;
  }

  // (COMMA argumentListPart)*
  private static boolean argumentList_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "argumentList_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!argumentList_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "argumentList_1", pos_)) break;
    }
    return true;
  }

  // COMMA argumentListPart
  private static boolean argumentList_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "argumentList_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, COMMA);
    result_ = result_ && argumentListPart(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // statement | expression
  static boolean argumentListPart(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "argumentListPart")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_);
    result_ = statement(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, AppleScriptParser::argListPartRecover);
    return result_;
  }

  /* ********************************************************** */
  // identifier COLON
  public static boolean argumentSelector(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "argumentSelector")) return false;
    if (!nextTokenIs(builder_, VAR_IDENTIFIER)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = identifier(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, COLON);
    exit_section_(builder_, marker_, ARGUMENT_SELECTOR, result_);
    return result_;
  }

  /* ********************************************************** */
  // <<parseExpression '"ASCII"' asciiObjectExpressionInner>>
  static boolean asciiObjectExpression(PsiBuilder builder_, int level_) {
    return parseExpression(builder_, level_ + 1, "ASCII", AppleScriptParser::asciiObjectExpressionInner);
  }

  /* ********************************************************** */
  // var_identifier (CHARACTER|NUMBER) expression
  static boolean asciiObjectExpressionInner(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "asciiObjectExpressionInner")) return false;
    if (!nextTokenIs(builder_, VAR_IDENTIFIER)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, VAR_IDENTIFIER);
    result_ = result_ && asciiObjectExpressionInner_1(builder_, level_ + 1);
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // CHARACTER|NUMBER
  private static boolean asciiObjectExpressionInner_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "asciiObjectExpressionInner_1")) return false;
    boolean result_;
    result_ = consumeToken(builder_, CHARACTER);
    if (!result_) result_ = consumeToken(builder_, NUMBER);
    return result_;
  }

  /* ********************************************************** */
  // setCommandAppleScript|copyCommandStatement
  public static boolean assignmentStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "assignmentStatement")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, ASSIGNMENT_STATEMENT, "<assignment statement>");
    result_ = setCommandAppleScript(builder_, level_ + 1);
    if (!result_) result_ = copyCommandStatement(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // blockBodyPart sep ( blockBodyPart sep)*
  public static boolean blockBody(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "blockBody")) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, BLOCK_BODY, "<block body>");
    result_ = blockBodyPart(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, sep(builder_, level_ + 1));
    result_ = pinned_ && blockBody_2(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // ( blockBodyPart sep)*
  private static boolean blockBody_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "blockBody_2")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!blockBody_2_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "blockBody_2", pos_)) break;
    }
    return true;
  }

  // blockBodyPart sep
  private static boolean blockBody_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "blockBody_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = blockBodyPart(builder_, level_ + 1);
    result_ = result_ && sep(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // statement|expression
  static boolean blockBodyPart(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "blockBodyPart")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_);
    result_ = statement(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, AppleScriptParser::bodyPartRecover);
    return result_;
  }

  /* ********************************************************** */
  // !(sep)
  static boolean bodyPartRecover(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "bodyPartRecover")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_);
    result_ = !bodyPartRecover_0(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // (sep)
  private static boolean bodyPartRecover_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "bodyPartRecover_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = sep(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // true|false
  static boolean boolean_constant(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "boolean_constant")) return false;
    if (!nextTokenIs(builder_, "", FALSE, TRUE)) return false;
    boolean result_;
    result_ = consumeToken(builder_, TRUE);
    if (!result_) result_ = consumeToken(builder_, FALSE);
    return result_;
  }

  /* ********************************************************** */
  // STRING|CLASS|CONSTANT|LIST|DATA|REFERENCE|STYLED_TEXT|TEXT_ITEM|ITEM|FILE_SPECIFICATION|
  // INTERNATIONAL_TEXT|RGB_COLOR|STYLED_CLIPBOARD_TEXT|UNICODE_TEXT|unitTypeValueClasses|CHARACTER|PARAGRAPH|WORD
  static boolean builtInClassIdCommon(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "builtInClassIdCommon")) return false;
    boolean result_;
    result_ = consumeToken(builder_, STRING);
    if (!result_) result_ = consumeToken(builder_, CLASS);
    if (!result_) result_ = consumeToken(builder_, CONSTANT);
    if (!result_) result_ = consumeToken(builder_, LIST);
    if (!result_) result_ = consumeToken(builder_, DATA);
    if (!result_) result_ = consumeToken(builder_, REFERENCE);
    if (!result_) result_ = consumeToken(builder_, STYLED_TEXT);
    if (!result_) result_ = consumeToken(builder_, TEXT_ITEM);
    if (!result_) result_ = consumeToken(builder_, ITEM);
    if (!result_) result_ = consumeToken(builder_, FILE_SPECIFICATION);
    if (!result_) result_ = consumeToken(builder_, INTERNATIONAL_TEXT);
    if (!result_) result_ = consumeToken(builder_, RGB_COLOR);
    if (!result_) result_ = consumeToken(builder_, STYLED_CLIPBOARD_TEXT);
    if (!result_) result_ = consumeToken(builder_, UNICODE_TEXT);
    if (!result_) result_ = unitTypeValueClasses(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, CHARACTER);
    if (!result_) result_ = consumeToken(builder_, PARAGRAPH);
    if (!result_) result_ = consumeToken(builder_, WORD);
    return result_;
  }

  /* ********************************************************** */
  // ANY|BOOLEAN|DATE|FILE|INTEGER|LOCATION_SPECIFIER|NUMBER|POINT|REAL|RECORD|RECTANGLE|SPECIFIER|TEXT|TYPE|ALIAS
  static boolean builtInClassIdNative(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "builtInClassIdNative")) return false;
    boolean result_;
    result_ = consumeToken(builder_, ANY);
    if (!result_) result_ = consumeToken(builder_, BOOLEAN);
    if (!result_) result_ = consumeToken(builder_, DATE);
    if (!result_) result_ = consumeToken(builder_, FILE);
    if (!result_) result_ = consumeToken(builder_, INTEGER);
    if (!result_) result_ = consumeToken(builder_, LOCATION_SPECIFIER);
    if (!result_) result_ = consumeToken(builder_, NUMBER);
    if (!result_) result_ = consumeToken(builder_, POINT);
    if (!result_) result_ = consumeToken(builder_, REAL);
    if (!result_) result_ = consumeToken(builder_, RECORD);
    if (!result_) result_ = consumeToken(builder_, RECTANGLE);
    if (!result_) result_ = consumeToken(builder_, SPECIFIER);
    if (!result_) result_ = consumeToken(builder_, TEXT);
    if (!result_) result_ = consumeToken(builder_, TYPE);
    if (!result_) result_ = consumeToken(builder_, ALIAS);
    return result_;
  }

  /* ********************************************************** */
  // builtInClassIdCommon|builtInClassIdNative|<<parseExpression '"script"' script>>
  // |<<parseExpression '"bundle"' bundle>>
  public static boolean builtInClassIdentifier(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "builtInClassIdentifier")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, BUILT_IN_CLASS_IDENTIFIER, "<built in class identifier>");
    result_ = builtInClassIdCommon(builder_, level_ + 1);
    if (!result_) result_ = builtInClassIdNative(builder_, level_ + 1);
    if (!result_) result_ = parseExpression(builder_, level_ + 1, "script", SCRIPT_parser_);
    if (!result_) result_ = parseExpression(builder_, level_ + 1, "bundle", BUNDLE_parser_);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // BUILT_IN_TYPE_S|scripts
  public static boolean builtInClassIdentifierPlural(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "builtInClassIdentifierPlural")) return false;
    if (!nextTokenIs(builder_, "<built in class identifier plural>", BUILT_IN_TYPE_S, SCRIPTS)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, BUILT_IN_CLASS_IDENTIFIER_PLURAL, "<built in class identifier plural>");
    result_ = consumeToken(builder_, BUILT_IN_TYPE_S);
    if (!result_) result_ = consumeToken(builder_, SCRIPTS);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // stringProperty | BUILT_IN_PROPERTY |
  // (&(builtInClassIdentifier of) builtInClassIdentifier)
  static boolean builtInClassProperty(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "builtInClassProperty")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = stringProperty(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, BUILT_IN_PROPERTY);
    if (!result_) result_ = builtInClassProperty_2(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // &(builtInClassIdentifier of) builtInClassIdentifier
  private static boolean builtInClassProperty_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "builtInClassProperty_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = builtInClassProperty_2_0(builder_, level_ + 1);
    result_ = result_ && builtInClassIdentifier(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // &(builtInClassIdentifier of)
  private static boolean builtInClassProperty_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "builtInClassProperty_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _AND_);
    result_ = builtInClassProperty_2_0_0(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // builtInClassIdentifier of
  private static boolean builtInClassProperty_2_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "builtInClassProperty_2_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = builtInClassIdentifier(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, OF);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // boolean_constant|date_time_constants|text_constant|itMeProperty
  // |currentApplicationConstant| missing_value_constant | scriptingAdditionsFolderConstant | currentDateConstant
  public static boolean builtInConstantLiteralExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "builtInConstantLiteralExpression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _COLLAPSE_, BUILT_IN_CONSTANT_LITERAL_EXPRESSION, "<built in constant literal expression>");
    result_ = boolean_constant(builder_, level_ + 1);
    if (!result_) result_ = date_time_constants(builder_, level_ + 1);
    if (!result_) result_ = text_constant(builder_, level_ + 1);
    if (!result_) result_ = itMeProperty(builder_, level_ + 1);
    if (!result_) result_ = currentApplicationConstant(builder_, level_ + 1);
    if (!result_) result_ = missing_value_constant(builder_, level_ + 1);
    if (!result_) result_ = scriptingAdditionsFolderConstant(builder_, level_ + 1);
    if (!result_) result_ = currentDateConstant(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // typeSpecifier
  static boolean classNamePrimaryExpression(PsiBuilder builder_, int level_) {
    return typeSpecifier(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // AS NLS* ( typeSpecifier | concatenationExpressionWrapper  )
  public static boolean coercionExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "coercionExpression")) return false;
    if (!nextTokenIsFast(builder_, AS)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, COERCION_EXPRESSION, null);
    result_ = consumeTokenFast(builder_, AS);
    result_ = result_ && coercionExpression_1(builder_, level_ + 1);
    result_ = result_ && coercionExpression_2(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // NLS*
  private static boolean coercionExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "coercionExpression_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!consumeTokenFast(builder_, NLS)) break;
      if (!empty_element_parsed_guard_(builder_, "coercionExpression_1", pos_)) break;
    }
    return true;
  }

  // typeSpecifier | concatenationExpressionWrapper
  private static boolean coercionExpression_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "coercionExpression_2")) return false;
    boolean result_;
    result_ = typeSpecifier(builder_, level_ + 1);
    if (!result_) result_ = concatenationExpressionWrapper(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // concatenationExpressionWrapper coercionExpression*
  static boolean coercionExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "coercionExpressionWrapper")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = concatenationExpressionWrapper(builder_, level_ + 1);
    result_ = result_ && coercionExpressionWrapper_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // coercionExpression*
  private static boolean coercionExpressionWrapper_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "coercionExpressionWrapper_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!coercionExpression(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "coercionExpressionWrapper_1", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // given? commandParameterSelector parameterValue
  public static boolean commandParameter(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "commandParameter")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, COMMAND_PARAMETER, "<command parameter>");
    result_ = commandParameter_0(builder_, level_ + 1);
    result_ = result_ && commandParameterSelector(builder_, level_ + 1);
    result_ = result_ && parameterValue(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // given?
  private static boolean commandParameter_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "commandParameter_0")) return false;
    consumeToken(builder_, GIVEN);
    return true;
  }

  /* ********************************************************** */
  // <<parseCommandParameterSelector>>
  public static boolean commandParameterSelector(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "commandParameterSelector")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, COMMAND_PARAMETER_SELECTOR, "<command parameter selector>");
    result_ = parseCommandParameterSelector(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // expression? (rawParameterExpression | givenRawParameterExpression)*
  static boolean commandRawParameters(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "commandRawParameters")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = commandRawParameters_0(builder_, level_ + 1);
    result_ = result_ && commandRawParameters_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // expression?
  private static boolean commandRawParameters_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "commandRawParameters_0")) return false;
    expression(builder_, level_ + 1);
    return true;
  }

  // (rawParameterExpression | givenRawParameterExpression)*
  private static boolean commandRawParameters_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "commandRawParameters_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!commandRawParameters_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "commandRawParameters_1", pos_)) break;
    }
    return true;
  }

  // rawParameterExpression | givenRawParameterExpression
  private static boolean commandRawParameters_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "commandRawParameters_1_0")) return false;
    boolean result_;
    result_ = rawParameterExpression(builder_, level_ + 1);
    if (!result_) result_ = givenRawParameterExpression(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // (equalityOperator|relational_operator) NLS* coercionExpressionWrapper
  public static boolean compareExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "compareExpression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, COMPARE_EXPRESSION, "<compare expression>");
    result_ = compareExpression_0(builder_, level_ + 1);
    result_ = result_ && compareExpression_1(builder_, level_ + 1);
    result_ = result_ && coercionExpressionWrapper(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // equalityOperator|relational_operator
  private static boolean compareExpression_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "compareExpression_0")) return false;
    boolean result_;
    result_ = equalityOperator(builder_, level_ + 1);
    if (!result_) result_ = relational_operator(builder_, level_ + 1);
    return result_;
  }

  // NLS*
  private static boolean compareExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "compareExpression_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!consumeTokenFast(builder_, NLS)) break;
      if (!empty_element_parsed_guard_(builder_, "compareExpression_1", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // coercionExpressionWrapper compareExpression*
  static boolean compareExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "compareExpressionWrapper")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = coercionExpressionWrapper(builder_, level_ + 1);
    result_ = result_ && compareExpressionWrapper_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // compareExpression*
  private static boolean compareExpressionWrapper_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "compareExpressionWrapper_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!compareExpression(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "compareExpressionWrapper_1", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // topBlockBodyPart|sep
  static boolean compilation_unit_(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "compilation_unit_")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = topBlockBodyPart(builder_, level_ + 1);
    if (!result_) result_ = sep(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // expression
  static boolean composite_value(PsiBuilder builder_, int level_) {
    return expression(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // BAND additiveExpressionWrapper
  public static boolean concatenationExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "concatenationExpression")) return false;
    if (!nextTokenIsFast(builder_, BAND)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, CONCATENATION_EXPRESSION, null);
    result_ = consumeTokenFast(builder_, BAND);
    result_ = result_ && additiveExpressionWrapper(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // additiveExpressionWrapper concatenationExpression*
  static boolean concatenationExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "concatenationExpressionWrapper")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = additiveExpressionWrapper(builder_, level_ + 1);
    result_ = result_ && concatenationExpressionWrapper_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // concatenationExpression*
  private static boolean concatenationExpressionWrapper_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "concatenationExpressionWrapper_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!concatenationExpression(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "concatenationExpressionWrapper_1", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // CONSIDER_IGNORE_ATTRIBUTE|C_WHITE_SPACE|expression
  static boolean considerOrIgnoreAttr(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "considerOrIgnoreAttr")) return false;
    boolean result_;
    result_ = consumeToken(builder_, CONSIDER_IGNORE_ATTRIBUTE);
    if (!result_) result_ = consumeToken(builder_, C_WHITE_SPACE);
    if (!result_) result_ = expression(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // considering considerOrIgnoreAttr ((COMMA considerOrIgnoreAttr)* (LAND considerOrIgnoreAttr)?)?
  //                            [but ignoring considerOrIgnoreAttr ((COMMA considerOrIgnoreAttr)* (LAND considerOrIgnoreAttr)?)? ] sep
  //                            blockBody?
  //                          end considering
  public static boolean consideringStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "consideringStatement")) return false;
    if (!nextTokenIs(builder_, CONSIDERING)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, CONSIDERING_STATEMENT, null);
    result_ = consumeToken(builder_, CONSIDERING);
    result_ = result_ && considerOrIgnoreAttr(builder_, level_ + 1);
    pinned_ = result_; // pin = 2
    result_ = result_ && report_error_(builder_, consideringStatement_2(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, consideringStatement_3(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, sep(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consideringStatement_5(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeTokens(builder_, -1, END, CONSIDERING)) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // ((COMMA considerOrIgnoreAttr)* (LAND considerOrIgnoreAttr)?)?
  private static boolean consideringStatement_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "consideringStatement_2")) return false;
    consideringStatement_2_0(builder_, level_ + 1);
    return true;
  }

  // (COMMA considerOrIgnoreAttr)* (LAND considerOrIgnoreAttr)?
  private static boolean consideringStatement_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "consideringStatement_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consideringStatement_2_0_0(builder_, level_ + 1);
    result_ = result_ && consideringStatement_2_0_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (COMMA considerOrIgnoreAttr)*
  private static boolean consideringStatement_2_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "consideringStatement_2_0_0")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!consideringStatement_2_0_0_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "consideringStatement_2_0_0", pos_)) break;
    }
    return true;
  }

  // COMMA considerOrIgnoreAttr
  private static boolean consideringStatement_2_0_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "consideringStatement_2_0_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, COMMA);
    result_ = result_ && considerOrIgnoreAttr(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (LAND considerOrIgnoreAttr)?
  private static boolean consideringStatement_2_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "consideringStatement_2_0_1")) return false;
    consideringStatement_2_0_1_0(builder_, level_ + 1);
    return true;
  }

  // LAND considerOrIgnoreAttr
  private static boolean consideringStatement_2_0_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "consideringStatement_2_0_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, LAND);
    result_ = result_ && considerOrIgnoreAttr(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // [but ignoring considerOrIgnoreAttr ((COMMA considerOrIgnoreAttr)* (LAND considerOrIgnoreAttr)?)? ]
  private static boolean consideringStatement_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "consideringStatement_3")) return false;
    consideringStatement_3_0(builder_, level_ + 1);
    return true;
  }

  // but ignoring considerOrIgnoreAttr ((COMMA considerOrIgnoreAttr)* (LAND considerOrIgnoreAttr)?)?
  private static boolean consideringStatement_3_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "consideringStatement_3_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokens(builder_, 0, BUT, IGNORING);
    result_ = result_ && considerOrIgnoreAttr(builder_, level_ + 1);
    result_ = result_ && consideringStatement_3_0_3(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // ((COMMA considerOrIgnoreAttr)* (LAND considerOrIgnoreAttr)?)?
  private static boolean consideringStatement_3_0_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "consideringStatement_3_0_3")) return false;
    consideringStatement_3_0_3_0(builder_, level_ + 1);
    return true;
  }

  // (COMMA considerOrIgnoreAttr)* (LAND considerOrIgnoreAttr)?
  private static boolean consideringStatement_3_0_3_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "consideringStatement_3_0_3_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consideringStatement_3_0_3_0_0(builder_, level_ + 1);
    result_ = result_ && consideringStatement_3_0_3_0_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (COMMA considerOrIgnoreAttr)*
  private static boolean consideringStatement_3_0_3_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "consideringStatement_3_0_3_0_0")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!consideringStatement_3_0_3_0_0_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "consideringStatement_3_0_3_0_0", pos_)) break;
    }
    return true;
  }

  // COMMA considerOrIgnoreAttr
  private static boolean consideringStatement_3_0_3_0_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "consideringStatement_3_0_3_0_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, COMMA);
    result_ = result_ && considerOrIgnoreAttr(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (LAND considerOrIgnoreAttr)?
  private static boolean consideringStatement_3_0_3_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "consideringStatement_3_0_3_0_1")) return false;
    consideringStatement_3_0_3_0_1_0(builder_, level_ + 1);
    return true;
  }

  // LAND considerOrIgnoreAttr
  private static boolean consideringStatement_3_0_3_0_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "consideringStatement_3_0_3_0_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, LAND);
    result_ = result_ && considerOrIgnoreAttr(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // blockBody?
  private static boolean consideringStatement_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "consideringStatement_5")) return false;
    blockBody(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // IS_CONTAIN|DOES_NOT_CONTAIN|IS_IN|IS_NOT_IN
  static boolean containment_any_part_operator(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "containment_any_part_operator")) return false;
    boolean result_;
    result_ = consumeToken(builder_, IS_CONTAIN);
    if (!result_) result_ = consumeToken(builder_, DOES_NOT_CONTAIN);
    if (!result_) result_ = consumeToken(builder_, IS_IN);
    if (!result_) result_ = consumeToken(builder_, IS_NOT_IN);
    return result_;
  }

  /* ********************************************************** */
  // STARTS_BEGINS_WITH|ENDS_WITH
  static boolean containment_start_end_operator(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "containment_start_end_operator")) return false;
    if (!nextTokenIs(builder_, "", ENDS_WITH, STARTS_BEGINS_WITH)) return false;
    boolean result_;
    result_ = consumeToken(builder_, STARTS_BEGINS_WITH);
    if (!result_) result_ = consumeToken(builder_, ENDS_WITH);
    return result_;
  }

  /* ********************************************************** */
  // continue expression
  public static boolean continue_statement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "continue_statement")) return false;
    if (!nextTokenIs(builder_, CONTINUE)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, CONTINUE);
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, CONTINUE_STATEMENT, result_);
    return result_;
  }

  /* ********************************************************** */
  // ifStatement | tryStatement | tellStatement | <<parseExpression '"repeat"' repeatStatement>>
  // | exitStatement | consideringStatement | ignoringStatement | withTimeoutStatement | withTransactionStatement | <<parseUsingTermsFromStatement>>
  static boolean controlStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "controlStatement")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = ifStatement(builder_, level_ + 1);
    if (!result_) result_ = tryStatement(builder_, level_ + 1);
    if (!result_) result_ = tellStatement(builder_, level_ + 1);
    if (!result_) result_ = parseExpression(builder_, level_ + 1, "repeat", AppleScriptParser::repeatStatement);
    if (!result_) result_ = exitStatement(builder_, level_ + 1);
    if (!result_) result_ = consideringStatement(builder_, level_ + 1);
    if (!result_) result_ = ignoringStatement(builder_, level_ + 1);
    if (!result_) result_ = withTimeoutStatement(builder_, level_ + 1);
    if (!result_) result_ = withTransactionStatement(builder_, level_ + 1);
    if (!result_) result_ = parseUsingTermsFromStatement(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // (copy|put) expression (to|into) (targetVariablePattern|objectReferenceWrapper)
  static boolean copyCommandStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "copyCommandStatement")) return false;
    if (!nextTokenIs(builder_, "", COPY, PUT)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = copyCommandStatement_0(builder_, level_ + 1);
    result_ = result_ && expression(builder_, level_ + 1);
    result_ = result_ && copyCommandStatement_2(builder_, level_ + 1);
    result_ = result_ && copyCommandStatement_3(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // copy|put
  private static boolean copyCommandStatement_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "copyCommandStatement_0")) return false;
    boolean result_;
    result_ = consumeToken(builder_, COPY);
    if (!result_) result_ = consumeToken(builder_, PUT);
    return result_;
  }

  // to|into
  private static boolean copyCommandStatement_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "copyCommandStatement_2")) return false;
    boolean result_;
    result_ = consumeToken(builder_, TO);
    if (!result_) result_ = consumeToken(builder_, INTO);
    return result_;
  }

  // targetVariablePattern|objectReferenceWrapper
  private static boolean copyCommandStatement_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "copyCommandStatement_3")) return false;
    boolean result_;
    result_ = targetVariablePattern(builder_, level_ + 1);
    if (!result_) result_ = objectReferenceWrapper(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // (count [[each|every] typeSpecifier (in|of)] composite_value) | (number of [typeSpecifier (in|of)] composite_value)
  public static boolean countCommandExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "countCommandExpression")) return false;
    if (!nextTokenIsFast(builder_, COUNT, NUMBER)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, COUNT_COMMAND_EXPRESSION, "<count command expression>");
    result_ = countCommandExpression_0(builder_, level_ + 1);
    if (!result_) result_ = countCommandExpression_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // count [[each|every] typeSpecifier (in|of)] composite_value
  private static boolean countCommandExpression_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "countCommandExpression_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, COUNT);
    result_ = result_ && countCommandExpression_0_1(builder_, level_ + 1);
    result_ = result_ && composite_value(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // [[each|every] typeSpecifier (in|of)]
  private static boolean countCommandExpression_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "countCommandExpression_0_1")) return false;
    countCommandExpression_0_1_0(builder_, level_ + 1);
    return true;
  }

  // [each|every] typeSpecifier (in|of)
  private static boolean countCommandExpression_0_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "countCommandExpression_0_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = countCommandExpression_0_1_0_0(builder_, level_ + 1);
    result_ = result_ && typeSpecifier(builder_, level_ + 1);
    result_ = result_ && countCommandExpression_0_1_0_2(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // [each|every]
  private static boolean countCommandExpression_0_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "countCommandExpression_0_1_0_0")) return false;
    countCommandExpression_0_1_0_0_0(builder_, level_ + 1);
    return true;
  }

  // each|every
  private static boolean countCommandExpression_0_1_0_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "countCommandExpression_0_1_0_0_0")) return false;
    boolean result_;
    result_ = consumeTokenFast(builder_, EACH);
    if (!result_) result_ = consumeTokenFast(builder_, EVERY);
    return result_;
  }

  // in|of
  private static boolean countCommandExpression_0_1_0_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "countCommandExpression_0_1_0_2")) return false;
    boolean result_;
    result_ = consumeTokenFast(builder_, IN);
    if (!result_) result_ = consumeTokenFast(builder_, OF);
    return result_;
  }

  // number of [typeSpecifier (in|of)] composite_value
  private static boolean countCommandExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "countCommandExpression_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokens(builder_, 0, NUMBER, OF);
    result_ = result_ && countCommandExpression_1_2(builder_, level_ + 1);
    result_ = result_ && composite_value(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // [typeSpecifier (in|of)]
  private static boolean countCommandExpression_1_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "countCommandExpression_1_2")) return false;
    countCommandExpression_1_2_0(builder_, level_ + 1);
    return true;
  }

  // typeSpecifier (in|of)
  private static boolean countCommandExpression_1_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "countCommandExpression_1_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = typeSpecifier(builder_, level_ + 1);
    result_ = result_ && countCommandExpression_1_2_0_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // in|of
  private static boolean countCommandExpression_1_2_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "countCommandExpression_1_2_0_1")) return false;
    boolean result_;
    result_ = consumeTokenFast(builder_, IN);
    if (!result_) result_ = consumeTokenFast(builder_, OF);
    return result_;
  }

  /* ********************************************************** */
  // CURRENT_APPLICATION|CURRENT_APP|(CURRENT APPLICATION)
  static boolean currentApplicationConstant(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "currentApplicationConstant")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, CURRENT_APPLICATION);
    if (!result_) result_ = consumeToken(builder_, CURRENT_APP);
    if (!result_) result_ = currentApplicationConstant_2(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // CURRENT APPLICATION
  private static boolean currentApplicationConstant_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "currentApplicationConstant_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokens(builder_, 0, CURRENT, APPLICATION);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // CURRENT DATE
  static boolean currentDateConstant(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "currentDateConstant")) return false;
    if (!nextTokenIs(builder_, CURRENT)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokens(builder_, 0, CURRENT, DATE);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // expression
  static boolean dataSpecifier(PsiBuilder builder_, int level_) {
    return expression(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // date (stringLiteralExpression|expression)
  public static boolean dateLiteralExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "dateLiteralExpression")) return false;
    if (!nextTokenIsFast(builder_, DATE)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, DATE);
    result_ = result_ && dateLiteralExpression_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, DATE_LITERAL_EXPRESSION, result_);
    return result_;
  }

  // stringLiteralExpression|expression
  private static boolean dateLiteralExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "dateLiteralExpression_1")) return false;
    boolean result_;
    result_ = stringLiteralExpression(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // weekday_constant|month_constant
  static boolean date_time_constants(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "date_time_constants")) return false;
    boolean result_;
    result_ = weekday_constant(builder_, level_ + 1);
    if (!result_) result_ = month_constant(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // ('.'DIGITS)|(DIGITS'.'DIGITS*)
  static boolean dec_significand(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "dec_significand")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = dec_significand_0(builder_, level_ + 1);
    if (!result_) result_ = dec_significand_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // '.'DIGITS
  private static boolean dec_significand_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "dec_significand_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, ".");
    result_ = result_ && consumeToken(builder_, DIGITS);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // DIGITS'.'DIGITS*
  private static boolean dec_significand_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "dec_significand_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, DIGITS);
    result_ = result_ && consumeToken(builder_, ".");
    result_ = result_ && dec_significand_1_2(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // DIGITS*
  private static boolean dec_significand_1_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "dec_significand_1_2")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!consumeToken(builder_, DIGITS)) break;
      if (!empty_element_parsed_guard_(builder_, "dec_significand_1_2", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // <<parseDictionaryClassName 'true' useStatementsCondition>>
  public static boolean dictionaryClassIdentifierPlural(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "dictionaryClassIdentifierPlural")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, DICTIONARY_CLASS_IDENTIFIER_PLURAL, "<dictionary class identifier plural>");
    result_ = parseDictionaryClassName(builder_, level_ + 1, true, AppleScriptParser::useStatementsCondition);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // <<parseDictionaryClassName 'false' useStatementsCondition>>
  public static boolean dictionaryClassName(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "dictionaryClassName")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, DICTIONARY_CLASS_NAME, "<dictionary class name>");
    result_ = parseDictionaryClassName(builder_, level_ + 1, false, AppleScriptParser::useStatementsCondition);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // <<parseCommandHandlerCallExpression>>
  public static boolean dictionaryCommandHandlerCallExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "dictionaryCommandHandlerCallExpression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _COLLAPSE_, DICTIONARY_COMMAND_HANDLER_CALL_EXPRESSION, "<dictionary command handler call expression>");
    result_ = parseCommandHandlerCallExpression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // <<parseDictionaryCommandName>>
  public static boolean dictionaryCommandName(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "dictionaryCommandName")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, DICTIONARY_COMMAND_NAME, "<dictionary command name>");
    result_ = parseDictionaryCommandName(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // <<parseDictionaryConstant>>
  public static boolean dictionaryConstant(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "dictionaryConstant")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, DICTIONARY_CONSTANT, "<dictionary constant>");
    result_ = parseDictionaryConstant(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // (<<parseDictionaryPropertyName>> &(of|in|COLON)) | (!dictionaryClassName <<parseDictionaryPropertyName>>)
  public static boolean dictionaryPropertyName(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "dictionaryPropertyName")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, DICTIONARY_PROPERTY_NAME, "<dictionary property name>");
    result_ = dictionaryPropertyName_0(builder_, level_ + 1);
    if (!result_) result_ = dictionaryPropertyName_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // <<parseDictionaryPropertyName>> &(of|in|COLON)
  private static boolean dictionaryPropertyName_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "dictionaryPropertyName_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = parseDictionaryPropertyName(builder_, level_ + 1);
    result_ = result_ && dictionaryPropertyName_0_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // &(of|in|COLON)
  private static boolean dictionaryPropertyName_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "dictionaryPropertyName_0_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _AND_);
    result_ = dictionaryPropertyName_0_1_0(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // of|in|COLON
  private static boolean dictionaryPropertyName_0_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "dictionaryPropertyName_0_1_0")) return false;
    boolean result_;
    result_ = consumeToken(builder_, OF);
    if (!result_) result_ = consumeToken(builder_, IN);
    if (!result_) result_ = consumeToken(builder_, COLON);
    return result_;
  }

  // !dictionaryClassName <<parseDictionaryPropertyName>>
  private static boolean dictionaryPropertyName_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "dictionaryPropertyName_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = dictionaryPropertyName_1_0(builder_, level_ + 1);
    result_ = result_ && parseDictionaryPropertyName(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // !dictionaryClassName
  private static boolean dictionaryPropertyName_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "dictionaryPropertyName_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_);
    result_ = !dictionaryClassName(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // THE_KW? identifier
  public static boolean directParameterDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "directParameterDeclaration")) return false;
    if (!nextTokenIs(builder_, "<direct parameter declaration>", THE_KW, VAR_IDENTIFIER)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, DIRECT_PARAMETER_DECLARATION, "<direct parameter declaration>");
    result_ = directParameterDeclaration_0(builder_, level_ + 1);
    result_ = result_ && identifier(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // THE_KW?
  private static boolean directParameterDeclaration_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "directParameterDeclaration_0")) return false;
    consumeToken(builder_, THE_KW);
    return true;
  }

  /* ********************************************************** */
  // referenceIdBeforeParamLabel | expression
  public static boolean directParameterVal(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "directParameterVal")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, DIRECT_PARAMETER_VAL, "<direct parameter val>");
    result_ = referenceIdBeforeParamLabel(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // NE|EQ
  static boolean equalityOperator(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "equalityOperator")) return false;
    if (!nextTokenIs(builder_, "", EQ, NE)) return false;
    boolean result_;
    result_ = consumeToken(builder_, NE);
    if (!result_) result_ = consumeToken(builder_, EQ);
    return result_;
  }

  /* ********************************************************** */
  // error [errorMessage] [number errorNumber]
  //                                  [from offendingObject]
  //                                  [to expectedType]
  //                                  [partial result resultList]
  public static boolean errorCommandExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "errorCommandExpression")) return false;
    if (!nextTokenIsFast(builder_, ERROR)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, ERROR);
    result_ = result_ && errorCommandExpression_1(builder_, level_ + 1);
    result_ = result_ && errorCommandExpression_2(builder_, level_ + 1);
    result_ = result_ && errorCommandExpression_3(builder_, level_ + 1);
    result_ = result_ && errorCommandExpression_4(builder_, level_ + 1);
    result_ = result_ && errorCommandExpression_5(builder_, level_ + 1);
    exit_section_(builder_, marker_, ERROR_COMMAND_EXPRESSION, result_);
    return result_;
  }

  // [errorMessage]
  private static boolean errorCommandExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "errorCommandExpression_1")) return false;
    errorMessage(builder_, level_ + 1);
    return true;
  }

  // [number errorNumber]
  private static boolean errorCommandExpression_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "errorCommandExpression_2")) return false;
    errorCommandExpression_2_0(builder_, level_ + 1);
    return true;
  }

  // number errorNumber
  private static boolean errorCommandExpression_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "errorCommandExpression_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, NUMBER);
    result_ = result_ && errorNumber(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // [from offendingObject]
  private static boolean errorCommandExpression_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "errorCommandExpression_3")) return false;
    errorCommandExpression_3_0(builder_, level_ + 1);
    return true;
  }

  // from offendingObject
  private static boolean errorCommandExpression_3_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "errorCommandExpression_3_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, FROM);
    result_ = result_ && offendingObject(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // [to expectedType]
  private static boolean errorCommandExpression_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "errorCommandExpression_4")) return false;
    errorCommandExpression_4_0(builder_, level_ + 1);
    return true;
  }

  // to expectedType
  private static boolean errorCommandExpression_4_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "errorCommandExpression_4_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, TO);
    result_ = result_ && expectedType(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // [partial result resultList]
  private static boolean errorCommandExpression_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "errorCommandExpression_5")) return false;
    errorCommandExpression_5_0(builder_, level_ + 1);
    return true;
  }

  // partial result resultList
  private static boolean errorCommandExpression_5_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "errorCommandExpression_5_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokens(builder_, 0, PARTIAL, RESULT);
    result_ = result_ && resultList(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // expression
  static boolean errorMessage(PsiBuilder builder_, int level_) {
    return expression(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // directParameterDeclaration|expression
  static boolean errorMessageVar(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "errorMessageVar")) return false;
    boolean result_;
    result_ = directParameterDeclaration(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // expression
  static boolean errorNumber(PsiBuilder builder_, int level_) {
    return expression(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // directParameterDeclaration|expression
  static boolean errorNumberVar(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "errorNumberVar")) return false;
    boolean result_;
    result_ = directParameterDeclaration(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // every typeSpecifier | pluralClassName
  public static boolean everyElemReference(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "everyElemReference")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, EVERY_ELEM_REFERENCE, "<every elem reference>");
    result_ = everyElemReference_0(builder_, level_ + 1);
    if (!result_) result_ = pluralClassName(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // every typeSpecifier
  private static boolean everyElemReference_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "everyElemReference_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, EVERY);
    result_ = result_ && typeSpecifier(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // every (singularClassName|userClassName) from (beginning|expression) to (end|expression)
  public static boolean everyRangeReference(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "everyRangeReference")) return false;
    if (!nextTokenIs(builder_, EVERY)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, EVERY);
    result_ = result_ && everyRangeReference_1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, FROM);
    result_ = result_ && everyRangeReference_3(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, TO);
    result_ = result_ && everyRangeReference_5(builder_, level_ + 1);
    exit_section_(builder_, marker_, EVERY_RANGE_REFERENCE, result_);
    return result_;
  }

  // singularClassName|userClassName
  private static boolean everyRangeReference_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "everyRangeReference_1")) return false;
    boolean result_;
    result_ = singularClassName(builder_, level_ + 1);
    if (!result_) result_ = userClassName(builder_, level_ + 1);
    return result_;
  }

  // beginning|expression
  private static boolean everyRangeReference_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "everyRangeReference_3")) return false;
    boolean result_;
    result_ = consumeToken(builder_, BEGINNING);
    if (!result_) result_ = expression(builder_, level_ + 1);
    return result_;
  }

  // end|expression
  private static boolean everyRangeReference_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "everyRangeReference_5")) return false;
    boolean result_;
    result_ = consumeToken(builder_, END);
    if (!result_) result_ = expression(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // exit [REPEAT]
  public static boolean exitStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "exitStatement")) return false;
    if (!nextTokenIs(builder_, EXIT)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, EXIT);
    result_ = result_ && exitStatement_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, EXIT_STATEMENT, result_);
    return result_;
  }

  // [REPEAT]
  private static boolean exitStatement_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "exitStatement_1")) return false;
    consumeToken(builder_, REPEAT);
    return true;
  }

  /* ********************************************************** */
  // expression
  static boolean expectedType(PsiBuilder builder_, int level_) {
    return expression(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // directParameterDeclaration|expression
  static boolean expectedTypeVar(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expectedTypeVar")) return false;
    boolean result_;
    result_ = directParameterDeclaration(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // logicalOrExpressionWrapper
  public static boolean expression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _COLLAPSE_, EXPRESSION, "<expression>");
    result_ = logicalOrExpressionWrapper(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // statement | expression
  static boolean expressionInParentheses(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expressionInParentheses")) return false;
    boolean result_;
    result_ = statement(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // (whose|where) expression
  public static boolean filterReference(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "filterReference")) return false;
    if (!nextTokenIs(builder_, "<filter reference>", WHERE, WHOSE)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, FILTER_REFERENCE, "<filter reference>");
    result_ = filterReference_0(builder_, level_ + 1);
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // whose|where
  private static boolean filterReference_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "filterReference_0")) return false;
    boolean result_;
    result_ = consumeToken(builder_, WHOSE);
    if (!result_) result_ = consumeToken(builder_, WHERE);
    return result_;
  }

  /* ********************************************************** */
  // formalParameterListPart (COMMA formalParameterListPart)*
  public static boolean formalParameterList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "formalParameterList")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, FORMAL_PARAMETER_LIST, "<formal parameter list>");
    result_ = formalParameterListPart(builder_, level_ + 1);
    result_ = result_ && formalParameterList_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // (COMMA formalParameterListPart)*
  private static boolean formalParameterList_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "formalParameterList_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!formalParameterList_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "formalParameterList_1", pos_)) break;
    }
    return true;
  }

  // COMMA formalParameterListPart
  private static boolean formalParameterList_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "formalParameterList_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, COMMA);
    result_ = result_ && formalParameterListPart(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // formalParameterListPartPattern
  static boolean formalParameterListPart(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "formalParameterListPart")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_);
    result_ = formalParameterListPartPattern(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, AppleScriptParser::argListPartRecover);
    return result_;
  }

  /* ********************************************************** */
  // simpleFormalParameter|listFormalParameter|recordFormalParameter|expression
  static boolean formalParameterListPartPattern(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "formalParameterListPartPattern")) return false;
    boolean result_;
    result_ = simpleFormalParameter(builder_, level_ + 1);
    if (!result_) result_ = listFormalParameter(builder_, level_ + 1);
    if (!result_) result_ = recordFormalParameter(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // get <<parseCommandParametersExpression>>
  public static boolean getCommandExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "getCommandExpression")) return false;
    if (!nextTokenIsFast(builder_, GET)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, GET);
    result_ = result_ && parseCommandParametersExpression(builder_, level_ + 1);
    exit_section_(builder_, marker_, GET_COMMAND_EXPRESSION, result_);
    return result_;
  }

  /* ********************************************************** */
  // given rawClassExpression COLON expression (COMMA rawClassExpression COLON expression)*
  public static boolean givenRawParameterExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "givenRawParameterExpression")) return false;
    if (!nextTokenIsFast(builder_, GIVEN)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, GIVEN);
    result_ = result_ && rawClassExpression(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, COLON);
    result_ = result_ && expression(builder_, level_ + 1);
    result_ = result_ && givenRawParameterExpression_4(builder_, level_ + 1);
    exit_section_(builder_, marker_, GIVEN_RAW_PARAMETER_EXPRESSION, result_);
    return result_;
  }

  // (COMMA rawClassExpression COLON expression)*
  private static boolean givenRawParameterExpression_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "givenRawParameterExpression_4")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!givenRawParameterExpression_4_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "givenRawParameterExpression_4", pos_)) break;
    }
    return true;
  }

  // COMMA rawClassExpression COLON expression
  private static boolean givenRawParameterExpression_4_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "givenRawParameterExpression_4_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, COMMA);
    result_ = result_ && rawClassExpression(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, COLON);
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // argumentSelector userParameterVal
  public static boolean handlerArgument(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerArgument")) return false;
    if (!nextTokenIs(builder_, VAR_IDENTIFIER)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = argumentSelector(builder_, level_ + 1);
    result_ = result_ && userParameterVal(builder_, level_ + 1);
    exit_section_(builder_, marker_, HANDLER_ARGUMENT, result_);
    return result_;
  }

  /* ********************************************************** */
  // handlerInterleavedParametersCall |
  // propertyReference | (referenceExpression handlerPositionalParametersCallExpression?)
  static boolean handlerInterleavedCallOrPropertyReferenceOrReferenceIdentifier(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerInterleavedCallOrPropertyReferenceOrReferenceIdentifier")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = handlerInterleavedParametersCall(builder_, level_ + 1);
    if (!result_) result_ = propertyReference(builder_, level_ + 1);
    if (!result_) result_ = handlerInterleavedCallOrPropertyReferenceOrReferenceIdentifier_2(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // referenceExpression handlerPositionalParametersCallExpression?
  private static boolean handlerInterleavedCallOrPropertyReferenceOrReferenceIdentifier_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerInterleavedCallOrPropertyReferenceOrReferenceIdentifier_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = referenceExpression(builder_, level_ + 1);
    result_ = result_ && handlerInterleavedCallOrPropertyReferenceOrReferenceIdentifier_2_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // handlerPositionalParametersCallExpression?
  private static boolean handlerInterleavedCallOrPropertyReferenceOrReferenceIdentifier_2_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerInterleavedCallOrPropertyReferenceOrReferenceIdentifier_2_1")) return false;
    handlerPositionalParametersCallExpression(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // handlerArgument+
  public static boolean handlerInterleavedParametersCall(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerInterleavedParametersCall")) return false;
    if (!nextTokenIs(builder_, VAR_IDENTIFIER)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = handlerArgument(builder_, level_ + 1);
    while (result_) {
      int pos_ = current_position_(builder_);
      if (!handlerArgument(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "handlerInterleavedParametersCall", pos_)) break;
    }
    exit_section_(builder_, marker_, HANDLER_INTERLEAVED_PARAMETERS_CALL, result_);
    return result_;
  }

  /* ********************************************************** */
  // (on|to) handlerInterleavedParametersSelectorPart+ sep
  //                                                blockBody?
  //                                              end [handlerNamePartRef COLON (handlerNamePartRef COLON)*]
  public static boolean handlerInterleavedParametersDefinition(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerInterleavedParametersDefinition")) return false;
    if (!nextTokenIs(builder_, "<handler interleaved parameters definition>", ON, TO)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, HANDLER_INTERLEAVED_PARAMETERS_DEFINITION, "<handler interleaved parameters definition>");
    result_ = handlerInterleavedParametersDefinition_0(builder_, level_ + 1);
    result_ = result_ && handlerInterleavedParametersDefinition_1(builder_, level_ + 1);
    result_ = result_ && sep(builder_, level_ + 1);
    result_ = result_ && handlerInterleavedParametersDefinition_3(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, END);
    result_ = result_ && handlerInterleavedParametersDefinition_5(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // on|to
  private static boolean handlerInterleavedParametersDefinition_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerInterleavedParametersDefinition_0")) return false;
    boolean result_;
    result_ = consumeToken(builder_, ON);
    if (!result_) result_ = consumeToken(builder_, TO);
    return result_;
  }

  // handlerInterleavedParametersSelectorPart+
  private static boolean handlerInterleavedParametersDefinition_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerInterleavedParametersDefinition_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = handlerInterleavedParametersSelectorPart(builder_, level_ + 1);
    while (result_) {
      int pos_ = current_position_(builder_);
      if (!handlerInterleavedParametersSelectorPart(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "handlerInterleavedParametersDefinition_1", pos_)) break;
    }
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // blockBody?
  private static boolean handlerInterleavedParametersDefinition_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerInterleavedParametersDefinition_3")) return false;
    blockBody(builder_, level_ + 1);
    return true;
  }

  // [handlerNamePartRef COLON (handlerNamePartRef COLON)*]
  private static boolean handlerInterleavedParametersDefinition_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerInterleavedParametersDefinition_5")) return false;
    handlerInterleavedParametersDefinition_5_0(builder_, level_ + 1);
    return true;
  }

  // handlerNamePartRef COLON (handlerNamePartRef COLON)*
  private static boolean handlerInterleavedParametersDefinition_5_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerInterleavedParametersDefinition_5_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = handlerNamePartRef(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, COLON);
    result_ = result_ && handlerInterleavedParametersDefinition_5_0_2(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (handlerNamePartRef COLON)*
  private static boolean handlerInterleavedParametersDefinition_5_0_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerInterleavedParametersDefinition_5_0_2")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!handlerInterleavedParametersDefinition_5_0_2_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "handlerInterleavedParametersDefinition_5_0_2", pos_)) break;
    }
    return true;
  }

  // handlerNamePartRef COLON
  private static boolean handlerInterleavedParametersDefinition_5_0_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerInterleavedParametersDefinition_5_0_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = handlerNamePartRef(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, COLON);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // selectorId COLON userParameterVar
  public static boolean handlerInterleavedParametersSelectorPart(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerInterleavedParametersSelectorPart")) return false;
    if (!nextTokenIs(builder_, VAR_IDENTIFIER)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = selectorId(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, COLON);
    result_ = result_ && userParameterVar(builder_, level_ + 1);
    exit_section_(builder_, marker_, HANDLER_INTERLEAVED_PARAMETERS_SELECTOR_PART, result_);
    return result_;
  }

  /* ********************************************************** */
  // (
  //    (of|in) directParameterVal// on|of is mandatory if parsing user handler call
  //    (
  //      (    (handlerParameterLabel parameterVal)+
  //            ( (with labelForTrueParam (COMMA labelForTrueParam)* [(LAND|LOR|COMMA) labelForTrueParam])
  //            | (without labelForFalseParam (COMMA labelForFalseParam)* [(LAND|LOR|COMMA) labelForFalseParam])
  //            | (given userLabelReference COLON userParameterVal (COMMA userLabelReference COLON userParameterVal)*)
  //            )*
  //      )
  //      |
  //        ( (with labelForTrueParam (COMMA labelForTrueParam)* [(LAND|LOR|COMMA) labelForTrueParam])
  //        | (without labelForFalseParam (COMMA labelForFalseParam)* [(LAND|LOR|COMMA) labelForFalseParam])
  //        | (given userLabelReference COLON userParameterVal (COMMA userLabelReference COLON userParameterVal)*)
  //        )+
  //    )
  //    )|
  //    (
  //    (handlerParameterLabel parameterVal)+ //&(with|without|given)
  //        ( (with labelForTrueParam (COMMA labelForTrueParam)* [(LAND|LOR|COMMA) labelForTrueParam])
  //        | (without labelForFalseParam (COMMA labelForFalseParam)* [(LAND|LOR|COMMA) labelForFalseParam])
  //        | (given userLabelReference COLON userParameterVal (COMMA userLabelReference COLON userParameterVal)*)
  //        )*
  //    )
  public static boolean handlerLabeledParametersCallExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, HANDLER_LABELED_PARAMETERS_CALL_EXPRESSION, "<handler labeled parameters call expression>");
    result_ = handlerLabeledParametersCallExpression_0(builder_, level_ + 1);
    if (!result_) result_ = handlerLabeledParametersCallExpression_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // (of|in) directParameterVal// on|of is mandatory if parsing user handler call
  //    (
  //      (    (handlerParameterLabel parameterVal)+
  //            ( (with labelForTrueParam (COMMA labelForTrueParam)* [(LAND|LOR|COMMA) labelForTrueParam])
  //            | (without labelForFalseParam (COMMA labelForFalseParam)* [(LAND|LOR|COMMA) labelForFalseParam])
  //            | (given userLabelReference COLON userParameterVal (COMMA userLabelReference COLON userParameterVal)*)
  //            )*
  //      )
  //      |
  //        ( (with labelForTrueParam (COMMA labelForTrueParam)* [(LAND|LOR|COMMA) labelForTrueParam])
  //        | (without labelForFalseParam (COMMA labelForFalseParam)* [(LAND|LOR|COMMA) labelForFalseParam])
  //        | (given userLabelReference COLON userParameterVal (COMMA userLabelReference COLON userParameterVal)*)
  //        )+
  //    )
  private static boolean handlerLabeledParametersCallExpression_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = handlerLabeledParametersCallExpression_0_0(builder_, level_ + 1);
    result_ = result_ && directParameterVal(builder_, level_ + 1);
    result_ = result_ && handlerLabeledParametersCallExpression_0_2(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // of|in
  private static boolean handlerLabeledParametersCallExpression_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_0")) return false;
    boolean result_;
    result_ = consumeTokenFast(builder_, OF);
    if (!result_) result_ = consumeTokenFast(builder_, IN);
    return result_;
  }

  // (    (handlerParameterLabel parameterVal)+
  //            ( (with labelForTrueParam (COMMA labelForTrueParam)* [(LAND|LOR|COMMA) labelForTrueParam])
  //            | (without labelForFalseParam (COMMA labelForFalseParam)* [(LAND|LOR|COMMA) labelForFalseParam])
  //            | (given userLabelReference COLON userParameterVal (COMMA userLabelReference COLON userParameterVal)*)
  //            )*
  //      )
  //      |
  //        ( (with labelForTrueParam (COMMA labelForTrueParam)* [(LAND|LOR|COMMA) labelForTrueParam])
  //        | (without labelForFalseParam (COMMA labelForFalseParam)* [(LAND|LOR|COMMA) labelForFalseParam])
  //        | (given userLabelReference COLON userParameterVal (COMMA userLabelReference COLON userParameterVal)*)
  //        )+
  private static boolean handlerLabeledParametersCallExpression_0_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = handlerLabeledParametersCallExpression_0_2_0(builder_, level_ + 1);
    if (!result_) result_ = handlerLabeledParametersCallExpression_0_2_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (handlerParameterLabel parameterVal)+
  //            ( (with labelForTrueParam (COMMA labelForTrueParam)* [(LAND|LOR|COMMA) labelForTrueParam])
  //            | (without labelForFalseParam (COMMA labelForFalseParam)* [(LAND|LOR|COMMA) labelForFalseParam])
  //            | (given userLabelReference COLON userParameterVal (COMMA userLabelReference COLON userParameterVal)*)
  //            )*
  private static boolean handlerLabeledParametersCallExpression_0_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = handlerLabeledParametersCallExpression_0_2_0_0(builder_, level_ + 1);
    result_ = result_ && handlerLabeledParametersCallExpression_0_2_0_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (handlerParameterLabel parameterVal)+
  private static boolean handlerLabeledParametersCallExpression_0_2_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = handlerLabeledParametersCallExpression_0_2_0_0_0(builder_, level_ + 1);
    while (result_) {
      int pos_ = current_position_(builder_);
      if (!handlerLabeledParametersCallExpression_0_2_0_0_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "handlerLabeledParametersCallExpression_0_2_0_0", pos_)) break;
    }
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // handlerParameterLabel parameterVal
  private static boolean handlerLabeledParametersCallExpression_0_2_0_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_0_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = handlerParameterLabel(builder_, level_ + 1);
    result_ = result_ && parameterVal(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // ( (with labelForTrueParam (COMMA labelForTrueParam)* [(LAND|LOR|COMMA) labelForTrueParam])
  //            | (without labelForFalseParam (COMMA labelForFalseParam)* [(LAND|LOR|COMMA) labelForFalseParam])
  //            | (given userLabelReference COLON userParameterVal (COMMA userLabelReference COLON userParameterVal)*)
  //            )*
  private static boolean handlerLabeledParametersCallExpression_0_2_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_0_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!handlerLabeledParametersCallExpression_0_2_0_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "handlerLabeledParametersCallExpression_0_2_0_1", pos_)) break;
    }
    return true;
  }

  // (with labelForTrueParam (COMMA labelForTrueParam)* [(LAND|LOR|COMMA) labelForTrueParam])
  //            | (without labelForFalseParam (COMMA labelForFalseParam)* [(LAND|LOR|COMMA) labelForFalseParam])
  //            | (given userLabelReference COLON userParameterVal (COMMA userLabelReference COLON userParameterVal)*)
  private static boolean handlerLabeledParametersCallExpression_0_2_0_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_0_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = handlerLabeledParametersCallExpression_0_2_0_1_0_0(builder_, level_ + 1);
    if (!result_) result_ = handlerLabeledParametersCallExpression_0_2_0_1_0_1(builder_, level_ + 1);
    if (!result_) result_ = handlerLabeledParametersCallExpression_0_2_0_1_0_2(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // with labelForTrueParam (COMMA labelForTrueParam)* [(LAND|LOR|COMMA) labelForTrueParam]
  private static boolean handlerLabeledParametersCallExpression_0_2_0_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_0_1_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, WITH);
    result_ = result_ && labelForTrueParam(builder_, level_ + 1);
    result_ = result_ && handlerLabeledParametersCallExpression_0_2_0_1_0_0_2(builder_, level_ + 1);
    result_ = result_ && handlerLabeledParametersCallExpression_0_2_0_1_0_0_3(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (COMMA labelForTrueParam)*
  private static boolean handlerLabeledParametersCallExpression_0_2_0_1_0_0_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_0_1_0_0_2")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!handlerLabeledParametersCallExpression_0_2_0_1_0_0_2_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "handlerLabeledParametersCallExpression_0_2_0_1_0_0_2", pos_)) break;
    }
    return true;
  }

  // COMMA labelForTrueParam
  private static boolean handlerLabeledParametersCallExpression_0_2_0_1_0_0_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_0_1_0_0_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, COMMA);
    result_ = result_ && labelForTrueParam(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // [(LAND|LOR|COMMA) labelForTrueParam]
  private static boolean handlerLabeledParametersCallExpression_0_2_0_1_0_0_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_0_1_0_0_3")) return false;
    handlerLabeledParametersCallExpression_0_2_0_1_0_0_3_0(builder_, level_ + 1);
    return true;
  }

  // (LAND|LOR|COMMA) labelForTrueParam
  private static boolean handlerLabeledParametersCallExpression_0_2_0_1_0_0_3_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_0_1_0_0_3_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = handlerLabeledParametersCallExpression_0_2_0_1_0_0_3_0_0(builder_, level_ + 1);
    result_ = result_ && labelForTrueParam(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // LAND|LOR|COMMA
  private static boolean handlerLabeledParametersCallExpression_0_2_0_1_0_0_3_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_0_1_0_0_3_0_0")) return false;
    boolean result_;
    result_ = consumeTokenFast(builder_, LAND);
    if (!result_) result_ = consumeTokenFast(builder_, LOR);
    if (!result_) result_ = consumeTokenFast(builder_, COMMA);
    return result_;
  }

  // without labelForFalseParam (COMMA labelForFalseParam)* [(LAND|LOR|COMMA) labelForFalseParam]
  private static boolean handlerLabeledParametersCallExpression_0_2_0_1_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_0_1_0_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, WITHOUT);
    result_ = result_ && labelForFalseParam(builder_, level_ + 1);
    result_ = result_ && handlerLabeledParametersCallExpression_0_2_0_1_0_1_2(builder_, level_ + 1);
    result_ = result_ && handlerLabeledParametersCallExpression_0_2_0_1_0_1_3(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (COMMA labelForFalseParam)*
  private static boolean handlerLabeledParametersCallExpression_0_2_0_1_0_1_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_0_1_0_1_2")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!handlerLabeledParametersCallExpression_0_2_0_1_0_1_2_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "handlerLabeledParametersCallExpression_0_2_0_1_0_1_2", pos_)) break;
    }
    return true;
  }

  // COMMA labelForFalseParam
  private static boolean handlerLabeledParametersCallExpression_0_2_0_1_0_1_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_0_1_0_1_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, COMMA);
    result_ = result_ && labelForFalseParam(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // [(LAND|LOR|COMMA) labelForFalseParam]
  private static boolean handlerLabeledParametersCallExpression_0_2_0_1_0_1_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_0_1_0_1_3")) return false;
    handlerLabeledParametersCallExpression_0_2_0_1_0_1_3_0(builder_, level_ + 1);
    return true;
  }

  // (LAND|LOR|COMMA) labelForFalseParam
  private static boolean handlerLabeledParametersCallExpression_0_2_0_1_0_1_3_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_0_1_0_1_3_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = handlerLabeledParametersCallExpression_0_2_0_1_0_1_3_0_0(builder_, level_ + 1);
    result_ = result_ && labelForFalseParam(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // LAND|LOR|COMMA
  private static boolean handlerLabeledParametersCallExpression_0_2_0_1_0_1_3_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_0_1_0_1_3_0_0")) return false;
    boolean result_;
    result_ = consumeTokenFast(builder_, LAND);
    if (!result_) result_ = consumeTokenFast(builder_, LOR);
    if (!result_) result_ = consumeTokenFast(builder_, COMMA);
    return result_;
  }

  // given userLabelReference COLON userParameterVal (COMMA userLabelReference COLON userParameterVal)*
  private static boolean handlerLabeledParametersCallExpression_0_2_0_1_0_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_0_1_0_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, GIVEN);
    result_ = result_ && userLabelReference(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, COLON);
    result_ = result_ && userParameterVal(builder_, level_ + 1);
    result_ = result_ && handlerLabeledParametersCallExpression_0_2_0_1_0_2_4(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (COMMA userLabelReference COLON userParameterVal)*
  private static boolean handlerLabeledParametersCallExpression_0_2_0_1_0_2_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_0_1_0_2_4")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!handlerLabeledParametersCallExpression_0_2_0_1_0_2_4_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "handlerLabeledParametersCallExpression_0_2_0_1_0_2_4", pos_)) break;
    }
    return true;
  }

  // COMMA userLabelReference COLON userParameterVal
  private static boolean handlerLabeledParametersCallExpression_0_2_0_1_0_2_4_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_0_1_0_2_4_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, COMMA);
    result_ = result_ && userLabelReference(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, COLON);
    result_ = result_ && userParameterVal(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // ( (with labelForTrueParam (COMMA labelForTrueParam)* [(LAND|LOR|COMMA) labelForTrueParam])
  //        | (without labelForFalseParam (COMMA labelForFalseParam)* [(LAND|LOR|COMMA) labelForFalseParam])
  //        | (given userLabelReference COLON userParameterVal (COMMA userLabelReference COLON userParameterVal)*)
  //        )+
  private static boolean handlerLabeledParametersCallExpression_0_2_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = handlerLabeledParametersCallExpression_0_2_1_0(builder_, level_ + 1);
    while (result_) {
      int pos_ = current_position_(builder_);
      if (!handlerLabeledParametersCallExpression_0_2_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "handlerLabeledParametersCallExpression_0_2_1", pos_)) break;
    }
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (with labelForTrueParam (COMMA labelForTrueParam)* [(LAND|LOR|COMMA) labelForTrueParam])
  //        | (without labelForFalseParam (COMMA labelForFalseParam)* [(LAND|LOR|COMMA) labelForFalseParam])
  //        | (given userLabelReference COLON userParameterVal (COMMA userLabelReference COLON userParameterVal)*)
  private static boolean handlerLabeledParametersCallExpression_0_2_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = handlerLabeledParametersCallExpression_0_2_1_0_0(builder_, level_ + 1);
    if (!result_) result_ = handlerLabeledParametersCallExpression_0_2_1_0_1(builder_, level_ + 1);
    if (!result_) result_ = handlerLabeledParametersCallExpression_0_2_1_0_2(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // with labelForTrueParam (COMMA labelForTrueParam)* [(LAND|LOR|COMMA) labelForTrueParam]
  private static boolean handlerLabeledParametersCallExpression_0_2_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_1_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, WITH);
    result_ = result_ && labelForTrueParam(builder_, level_ + 1);
    result_ = result_ && handlerLabeledParametersCallExpression_0_2_1_0_0_2(builder_, level_ + 1);
    result_ = result_ && handlerLabeledParametersCallExpression_0_2_1_0_0_3(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (COMMA labelForTrueParam)*
  private static boolean handlerLabeledParametersCallExpression_0_2_1_0_0_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_1_0_0_2")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!handlerLabeledParametersCallExpression_0_2_1_0_0_2_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "handlerLabeledParametersCallExpression_0_2_1_0_0_2", pos_)) break;
    }
    return true;
  }

  // COMMA labelForTrueParam
  private static boolean handlerLabeledParametersCallExpression_0_2_1_0_0_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_1_0_0_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, COMMA);
    result_ = result_ && labelForTrueParam(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // [(LAND|LOR|COMMA) labelForTrueParam]
  private static boolean handlerLabeledParametersCallExpression_0_2_1_0_0_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_1_0_0_3")) return false;
    handlerLabeledParametersCallExpression_0_2_1_0_0_3_0(builder_, level_ + 1);
    return true;
  }

  // (LAND|LOR|COMMA) labelForTrueParam
  private static boolean handlerLabeledParametersCallExpression_0_2_1_0_0_3_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_1_0_0_3_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = handlerLabeledParametersCallExpression_0_2_1_0_0_3_0_0(builder_, level_ + 1);
    result_ = result_ && labelForTrueParam(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // LAND|LOR|COMMA
  private static boolean handlerLabeledParametersCallExpression_0_2_1_0_0_3_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_1_0_0_3_0_0")) return false;
    boolean result_;
    result_ = consumeTokenFast(builder_, LAND);
    if (!result_) result_ = consumeTokenFast(builder_, LOR);
    if (!result_) result_ = consumeTokenFast(builder_, COMMA);
    return result_;
  }

  // without labelForFalseParam (COMMA labelForFalseParam)* [(LAND|LOR|COMMA) labelForFalseParam]
  private static boolean handlerLabeledParametersCallExpression_0_2_1_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_1_0_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, WITHOUT);
    result_ = result_ && labelForFalseParam(builder_, level_ + 1);
    result_ = result_ && handlerLabeledParametersCallExpression_0_2_1_0_1_2(builder_, level_ + 1);
    result_ = result_ && handlerLabeledParametersCallExpression_0_2_1_0_1_3(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (COMMA labelForFalseParam)*
  private static boolean handlerLabeledParametersCallExpression_0_2_1_0_1_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_1_0_1_2")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!handlerLabeledParametersCallExpression_0_2_1_0_1_2_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "handlerLabeledParametersCallExpression_0_2_1_0_1_2", pos_)) break;
    }
    return true;
  }

  // COMMA labelForFalseParam
  private static boolean handlerLabeledParametersCallExpression_0_2_1_0_1_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_1_0_1_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, COMMA);
    result_ = result_ && labelForFalseParam(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // [(LAND|LOR|COMMA) labelForFalseParam]
  private static boolean handlerLabeledParametersCallExpression_0_2_1_0_1_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_1_0_1_3")) return false;
    handlerLabeledParametersCallExpression_0_2_1_0_1_3_0(builder_, level_ + 1);
    return true;
  }

  // (LAND|LOR|COMMA) labelForFalseParam
  private static boolean handlerLabeledParametersCallExpression_0_2_1_0_1_3_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_1_0_1_3_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = handlerLabeledParametersCallExpression_0_2_1_0_1_3_0_0(builder_, level_ + 1);
    result_ = result_ && labelForFalseParam(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // LAND|LOR|COMMA
  private static boolean handlerLabeledParametersCallExpression_0_2_1_0_1_3_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_1_0_1_3_0_0")) return false;
    boolean result_;
    result_ = consumeTokenFast(builder_, LAND);
    if (!result_) result_ = consumeTokenFast(builder_, LOR);
    if (!result_) result_ = consumeTokenFast(builder_, COMMA);
    return result_;
  }

  // given userLabelReference COLON userParameterVal (COMMA userLabelReference COLON userParameterVal)*
  private static boolean handlerLabeledParametersCallExpression_0_2_1_0_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_1_0_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, GIVEN);
    result_ = result_ && userLabelReference(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, COLON);
    result_ = result_ && userParameterVal(builder_, level_ + 1);
    result_ = result_ && handlerLabeledParametersCallExpression_0_2_1_0_2_4(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (COMMA userLabelReference COLON userParameterVal)*
  private static boolean handlerLabeledParametersCallExpression_0_2_1_0_2_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_1_0_2_4")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!handlerLabeledParametersCallExpression_0_2_1_0_2_4_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "handlerLabeledParametersCallExpression_0_2_1_0_2_4", pos_)) break;
    }
    return true;
  }

  // COMMA userLabelReference COLON userParameterVal
  private static boolean handlerLabeledParametersCallExpression_0_2_1_0_2_4_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_0_2_1_0_2_4_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, COMMA);
    result_ = result_ && userLabelReference(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, COLON);
    result_ = result_ && userParameterVal(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (handlerParameterLabel parameterVal)+ //&(with|without|given)
  //        ( (with labelForTrueParam (COMMA labelForTrueParam)* [(LAND|LOR|COMMA) labelForTrueParam])
  //        | (without labelForFalseParam (COMMA labelForFalseParam)* [(LAND|LOR|COMMA) labelForFalseParam])
  //        | (given userLabelReference COLON userParameterVal (COMMA userLabelReference COLON userParameterVal)*)
  //        )*
  private static boolean handlerLabeledParametersCallExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = handlerLabeledParametersCallExpression_1_0(builder_, level_ + 1);
    result_ = result_ && handlerLabeledParametersCallExpression_1_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (handlerParameterLabel parameterVal)+
  private static boolean handlerLabeledParametersCallExpression_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = handlerLabeledParametersCallExpression_1_0_0(builder_, level_ + 1);
    while (result_) {
      int pos_ = current_position_(builder_);
      if (!handlerLabeledParametersCallExpression_1_0_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "handlerLabeledParametersCallExpression_1_0", pos_)) break;
    }
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // handlerParameterLabel parameterVal
  private static boolean handlerLabeledParametersCallExpression_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_1_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = handlerParameterLabel(builder_, level_ + 1);
    result_ = result_ && parameterVal(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // ( (with labelForTrueParam (COMMA labelForTrueParam)* [(LAND|LOR|COMMA) labelForTrueParam])
  //        | (without labelForFalseParam (COMMA labelForFalseParam)* [(LAND|LOR|COMMA) labelForFalseParam])
  //        | (given userLabelReference COLON userParameterVal (COMMA userLabelReference COLON userParameterVal)*)
  //        )*
  private static boolean handlerLabeledParametersCallExpression_1_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_1_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!handlerLabeledParametersCallExpression_1_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "handlerLabeledParametersCallExpression_1_1", pos_)) break;
    }
    return true;
  }

  // (with labelForTrueParam (COMMA labelForTrueParam)* [(LAND|LOR|COMMA) labelForTrueParam])
  //        | (without labelForFalseParam (COMMA labelForFalseParam)* [(LAND|LOR|COMMA) labelForFalseParam])
  //        | (given userLabelReference COLON userParameterVal (COMMA userLabelReference COLON userParameterVal)*)
  private static boolean handlerLabeledParametersCallExpression_1_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_1_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = handlerLabeledParametersCallExpression_1_1_0_0(builder_, level_ + 1);
    if (!result_) result_ = handlerLabeledParametersCallExpression_1_1_0_1(builder_, level_ + 1);
    if (!result_) result_ = handlerLabeledParametersCallExpression_1_1_0_2(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // with labelForTrueParam (COMMA labelForTrueParam)* [(LAND|LOR|COMMA) labelForTrueParam]
  private static boolean handlerLabeledParametersCallExpression_1_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_1_1_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, WITH);
    result_ = result_ && labelForTrueParam(builder_, level_ + 1);
    result_ = result_ && handlerLabeledParametersCallExpression_1_1_0_0_2(builder_, level_ + 1);
    result_ = result_ && handlerLabeledParametersCallExpression_1_1_0_0_3(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (COMMA labelForTrueParam)*
  private static boolean handlerLabeledParametersCallExpression_1_1_0_0_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_1_1_0_0_2")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!handlerLabeledParametersCallExpression_1_1_0_0_2_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "handlerLabeledParametersCallExpression_1_1_0_0_2", pos_)) break;
    }
    return true;
  }

  // COMMA labelForTrueParam
  private static boolean handlerLabeledParametersCallExpression_1_1_0_0_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_1_1_0_0_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, COMMA);
    result_ = result_ && labelForTrueParam(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // [(LAND|LOR|COMMA) labelForTrueParam]
  private static boolean handlerLabeledParametersCallExpression_1_1_0_0_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_1_1_0_0_3")) return false;
    handlerLabeledParametersCallExpression_1_1_0_0_3_0(builder_, level_ + 1);
    return true;
  }

  // (LAND|LOR|COMMA) labelForTrueParam
  private static boolean handlerLabeledParametersCallExpression_1_1_0_0_3_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_1_1_0_0_3_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = handlerLabeledParametersCallExpression_1_1_0_0_3_0_0(builder_, level_ + 1);
    result_ = result_ && labelForTrueParam(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // LAND|LOR|COMMA
  private static boolean handlerLabeledParametersCallExpression_1_1_0_0_3_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_1_1_0_0_3_0_0")) return false;
    boolean result_;
    result_ = consumeTokenFast(builder_, LAND);
    if (!result_) result_ = consumeTokenFast(builder_, LOR);
    if (!result_) result_ = consumeTokenFast(builder_, COMMA);
    return result_;
  }

  // without labelForFalseParam (COMMA labelForFalseParam)* [(LAND|LOR|COMMA) labelForFalseParam]
  private static boolean handlerLabeledParametersCallExpression_1_1_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_1_1_0_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, WITHOUT);
    result_ = result_ && labelForFalseParam(builder_, level_ + 1);
    result_ = result_ && handlerLabeledParametersCallExpression_1_1_0_1_2(builder_, level_ + 1);
    result_ = result_ && handlerLabeledParametersCallExpression_1_1_0_1_3(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (COMMA labelForFalseParam)*
  private static boolean handlerLabeledParametersCallExpression_1_1_0_1_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_1_1_0_1_2")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!handlerLabeledParametersCallExpression_1_1_0_1_2_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "handlerLabeledParametersCallExpression_1_1_0_1_2", pos_)) break;
    }
    return true;
  }

  // COMMA labelForFalseParam
  private static boolean handlerLabeledParametersCallExpression_1_1_0_1_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_1_1_0_1_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, COMMA);
    result_ = result_ && labelForFalseParam(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // [(LAND|LOR|COMMA) labelForFalseParam]
  private static boolean handlerLabeledParametersCallExpression_1_1_0_1_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_1_1_0_1_3")) return false;
    handlerLabeledParametersCallExpression_1_1_0_1_3_0(builder_, level_ + 1);
    return true;
  }

  // (LAND|LOR|COMMA) labelForFalseParam
  private static boolean handlerLabeledParametersCallExpression_1_1_0_1_3_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_1_1_0_1_3_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = handlerLabeledParametersCallExpression_1_1_0_1_3_0_0(builder_, level_ + 1);
    result_ = result_ && labelForFalseParam(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // LAND|LOR|COMMA
  private static boolean handlerLabeledParametersCallExpression_1_1_0_1_3_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_1_1_0_1_3_0_0")) return false;
    boolean result_;
    result_ = consumeTokenFast(builder_, LAND);
    if (!result_) result_ = consumeTokenFast(builder_, LOR);
    if (!result_) result_ = consumeTokenFast(builder_, COMMA);
    return result_;
  }

  // given userLabelReference COLON userParameterVal (COMMA userLabelReference COLON userParameterVal)*
  private static boolean handlerLabeledParametersCallExpression_1_1_0_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_1_1_0_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, GIVEN);
    result_ = result_ && userLabelReference(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, COLON);
    result_ = result_ && userParameterVal(builder_, level_ + 1);
    result_ = result_ && handlerLabeledParametersCallExpression_1_1_0_2_4(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (COMMA userLabelReference COLON userParameterVal)*
  private static boolean handlerLabeledParametersCallExpression_1_1_0_2_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_1_1_0_2_4")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!handlerLabeledParametersCallExpression_1_1_0_2_4_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "handlerLabeledParametersCallExpression_1_1_0_2_4", pos_)) break;
    }
    return true;
  }

  // COMMA userLabelReference COLON userParameterVal
  private static boolean handlerLabeledParametersCallExpression_1_1_0_2_4_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersCallExpression_1_1_0_2_4_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, COMMA);
    result_ = result_ && userLabelReference(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, COLON);
    result_ = result_ && userParameterVal(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // (on|to) identifier !LPAREN labeledParameterDeclarationList
  //                                          [given THE_KW? objectTargetPropertyDeclaration (COMMA THE_KW? objectTargetPropertyDeclaration)*]
  //                                          [varDeclarationList] sep
  //                                            blockBody?
  //                                         end [referenceExpression]
  public static boolean handlerLabeledParametersDefinition(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersDefinition")) return false;
    if (!nextTokenIs(builder_, "<handler labeled parameters definition>", ON, TO)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, HANDLER_LABELED_PARAMETERS_DEFINITION, "<handler labeled parameters definition>");
    result_ = handlerLabeledParametersDefinition_0(builder_, level_ + 1);
    result_ = result_ && identifier(builder_, level_ + 1);
    result_ = result_ && handlerLabeledParametersDefinition_2(builder_, level_ + 1);
    result_ = result_ && labeledParameterDeclarationList(builder_, level_ + 1);
    result_ = result_ && handlerLabeledParametersDefinition_4(builder_, level_ + 1);
    result_ = result_ && handlerLabeledParametersDefinition_5(builder_, level_ + 1);
    result_ = result_ && sep(builder_, level_ + 1);
    result_ = result_ && handlerLabeledParametersDefinition_7(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, END);
    result_ = result_ && handlerLabeledParametersDefinition_9(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // on|to
  private static boolean handlerLabeledParametersDefinition_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersDefinition_0")) return false;
    boolean result_;
    result_ = consumeToken(builder_, ON);
    if (!result_) result_ = consumeToken(builder_, TO);
    return result_;
  }

  // !LPAREN
  private static boolean handlerLabeledParametersDefinition_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersDefinition_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_);
    result_ = !consumeToken(builder_, LPAREN);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // [given THE_KW? objectTargetPropertyDeclaration (COMMA THE_KW? objectTargetPropertyDeclaration)*]
  private static boolean handlerLabeledParametersDefinition_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersDefinition_4")) return false;
    handlerLabeledParametersDefinition_4_0(builder_, level_ + 1);
    return true;
  }

  // given THE_KW? objectTargetPropertyDeclaration (COMMA THE_KW? objectTargetPropertyDeclaration)*
  private static boolean handlerLabeledParametersDefinition_4_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersDefinition_4_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, GIVEN);
    result_ = result_ && handlerLabeledParametersDefinition_4_0_1(builder_, level_ + 1);
    result_ = result_ && objectTargetPropertyDeclaration(builder_, level_ + 1);
    result_ = result_ && handlerLabeledParametersDefinition_4_0_3(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // THE_KW?
  private static boolean handlerLabeledParametersDefinition_4_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersDefinition_4_0_1")) return false;
    consumeToken(builder_, THE_KW);
    return true;
  }

  // (COMMA THE_KW? objectTargetPropertyDeclaration)*
  private static boolean handlerLabeledParametersDefinition_4_0_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersDefinition_4_0_3")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!handlerLabeledParametersDefinition_4_0_3_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "handlerLabeledParametersDefinition_4_0_3", pos_)) break;
    }
    return true;
  }

  // COMMA THE_KW? objectTargetPropertyDeclaration
  private static boolean handlerLabeledParametersDefinition_4_0_3_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersDefinition_4_0_3_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, COMMA);
    result_ = result_ && handlerLabeledParametersDefinition_4_0_3_0_1(builder_, level_ + 1);
    result_ = result_ && objectTargetPropertyDeclaration(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // THE_KW?
  private static boolean handlerLabeledParametersDefinition_4_0_3_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersDefinition_4_0_3_0_1")) return false;
    consumeToken(builder_, THE_KW);
    return true;
  }

  // [varDeclarationList]
  private static boolean handlerLabeledParametersDefinition_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersDefinition_5")) return false;
    varDeclarationList(builder_, level_ + 1);
    return true;
  }

  // blockBody?
  private static boolean handlerLabeledParametersDefinition_7(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersDefinition_7")) return false;
    blockBody(builder_, level_ + 1);
    return true;
  }

  // [referenceExpression]
  private static boolean handlerLabeledParametersDefinition_9(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerLabeledParametersDefinition_9")) return false;
    referenceExpression(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // referenceExpression
  static boolean handlerNamePartRef(PsiBuilder builder_, int level_) {
    return referenceExpression(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // about|above|against|'apart from'|around|'aside from'|at|below|beneath|beside|between|by|for
  // |from|'instead of'|into|on|onto|'out of'|over|since|thru|through|under|to
  public static boolean handlerParameterLabel(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerParameterLabel")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, HANDLER_PARAMETER_LABEL, "<handler parameter label>");
    result_ = consumeToken(builder_, ABOUT);
    if (!result_) result_ = consumeToken(builder_, ABOVE);
    if (!result_) result_ = consumeToken(builder_, AGAINST);
    if (!result_) result_ = consumeToken(builder_, "apart from");
    if (!result_) result_ = consumeToken(builder_, AROUND);
    if (!result_) result_ = consumeToken(builder_, "aside from");
    if (!result_) result_ = consumeToken(builder_, AT);
    if (!result_) result_ = consumeToken(builder_, BELOW);
    if (!result_) result_ = consumeToken(builder_, BENEATH);
    if (!result_) result_ = consumeToken(builder_, BESIDE);
    if (!result_) result_ = consumeToken(builder_, BETWEEN);
    if (!result_) result_ = consumeToken(builder_, BY);
    if (!result_) result_ = consumeToken(builder_, FOR);
    if (!result_) result_ = consumeToken(builder_, FROM);
    if (!result_) result_ = consumeToken(builder_, "instead of");
    if (!result_) result_ = consumeToken(builder_, INTO);
    if (!result_) result_ = consumeToken(builder_, ON);
    if (!result_) result_ = consumeToken(builder_, ONTO);
    if (!result_) result_ = consumeToken(builder_, "out of");
    if (!result_) result_ = consumeToken(builder_, OVER);
    if (!result_) result_ = consumeToken(builder_, SINCE);
    if (!result_) result_ = consumeToken(builder_, THRU);
    if (!result_) result_ = consumeToken(builder_, THROUGH);
    if (!result_) result_ = consumeToken(builder_, UNDER);
    if (!result_) result_ = consumeToken(builder_, TO);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // LPAREN argumentList? RPAREN
  public static boolean handlerPositionalParametersCallExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerPositionalParametersCallExpression")) return false;
    if (!nextTokenIsFast(builder_, LPAREN)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, HANDLER_POSITIONAL_PARAMETERS_CALL_EXPRESSION, null);
    result_ = consumeTokenFast(builder_, LPAREN);
    result_ = result_ && handlerPositionalParametersCallExpression_1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, RPAREN);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // argumentList?
  private static boolean handlerPositionalParametersCallExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerPositionalParametersCallExpression_1")) return false;
    argumentList(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // (on|to) identifier LPAREN [formalParameterList] RPAREN
  //                                             [varDeclarationList] sep
  //                                                blockBody?
  //                                             end [referenceExpression]
  public static boolean handlerPositionalParametersDefinition(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerPositionalParametersDefinition")) return false;
    if (!nextTokenIs(builder_, "<handler positional parameters definition>", ON, TO)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, HANDLER_POSITIONAL_PARAMETERS_DEFINITION, "<handler positional parameters definition>");
    result_ = handlerPositionalParametersDefinition_0(builder_, level_ + 1);
    result_ = result_ && identifier(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, LPAREN);
    result_ = result_ && handlerPositionalParametersDefinition_3(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, RPAREN);
    result_ = result_ && handlerPositionalParametersDefinition_5(builder_, level_ + 1);
    result_ = result_ && sep(builder_, level_ + 1);
    result_ = result_ && handlerPositionalParametersDefinition_7(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, END);
    result_ = result_ && handlerPositionalParametersDefinition_9(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // on|to
  private static boolean handlerPositionalParametersDefinition_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerPositionalParametersDefinition_0")) return false;
    boolean result_;
    result_ = consumeToken(builder_, ON);
    if (!result_) result_ = consumeToken(builder_, TO);
    return result_;
  }

  // [formalParameterList]
  private static boolean handlerPositionalParametersDefinition_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerPositionalParametersDefinition_3")) return false;
    formalParameterList(builder_, level_ + 1);
    return true;
  }

  // [varDeclarationList]
  private static boolean handlerPositionalParametersDefinition_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerPositionalParametersDefinition_5")) return false;
    varDeclarationList(builder_, level_ + 1);
    return true;
  }

  // blockBody?
  private static boolean handlerPositionalParametersDefinition_7(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerPositionalParametersDefinition_7")) return false;
    blockBody(builder_, level_ + 1);
    return true;
  }

  // [referenceExpression]
  private static boolean handlerPositionalParametersDefinition_9(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "handlerPositionalParametersDefinition_9")) return false;
    referenceExpression(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // id expression
  public static boolean idReference(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "idReference")) return false;
    if (!nextTokenIs(builder_, ID)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, ID_REFERENCE, null);
    result_ = consumeToken(builder_, ID);
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // var_identifier
  public static boolean identifier(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "identifier")) return false;
    if (!nextTokenIs(builder_, VAR_IDENTIFIER)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, VAR_IDENTIFIER);
    exit_section_(builder_, marker_, IDENTIFIER, result_);
    return result_;
  }

  /* ********************************************************** */
  // if expression [then] sep
  //                                        blockBody?
  //                                    (else if expression [then] sep
  //                                        blockBody?  )*
  //                                    (else sep
  //                                         blockBody? )?
  //                         end [if]
  public static boolean ifCompoundStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ifCompoundStatement")) return false;
    if (!nextTokenIs(builder_, IF)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, IF_COMPOUND_STATEMENT, null);
    result_ = consumeToken(builder_, IF);
    result_ = result_ && expression(builder_, level_ + 1);
    result_ = result_ && ifCompoundStatement_2(builder_, level_ + 1);
    result_ = result_ && sep(builder_, level_ + 1);
    result_ = result_ && ifCompoundStatement_4(builder_, level_ + 1);
    result_ = result_ && ifCompoundStatement_5(builder_, level_ + 1);
    result_ = result_ && ifCompoundStatement_6(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, END);
    pinned_ = result_; // pin = 8
    result_ = result_ && ifCompoundStatement_8(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // [then]
  private static boolean ifCompoundStatement_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ifCompoundStatement_2")) return false;
    consumeToken(builder_, THEN);
    return true;
  }

  // blockBody?
  private static boolean ifCompoundStatement_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ifCompoundStatement_4")) return false;
    blockBody(builder_, level_ + 1);
    return true;
  }

  // (else if expression [then] sep
  //                                        blockBody?  )*
  private static boolean ifCompoundStatement_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ifCompoundStatement_5")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!ifCompoundStatement_5_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "ifCompoundStatement_5", pos_)) break;
    }
    return true;
  }

  // else if expression [then] sep
  //                                        blockBody?
  private static boolean ifCompoundStatement_5_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ifCompoundStatement_5_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokens(builder_, 0, ELSE, IF);
    result_ = result_ && expression(builder_, level_ + 1);
    result_ = result_ && ifCompoundStatement_5_0_3(builder_, level_ + 1);
    result_ = result_ && sep(builder_, level_ + 1);
    result_ = result_ && ifCompoundStatement_5_0_5(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // [then]
  private static boolean ifCompoundStatement_5_0_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ifCompoundStatement_5_0_3")) return false;
    consumeToken(builder_, THEN);
    return true;
  }

  // blockBody?
  private static boolean ifCompoundStatement_5_0_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ifCompoundStatement_5_0_5")) return false;
    blockBody(builder_, level_ + 1);
    return true;
  }

  // (else sep
  //                                         blockBody? )?
  private static boolean ifCompoundStatement_6(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ifCompoundStatement_6")) return false;
    ifCompoundStatement_6_0(builder_, level_ + 1);
    return true;
  }

  // else sep
  //                                         blockBody?
  private static boolean ifCompoundStatement_6_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ifCompoundStatement_6_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, ELSE);
    result_ = result_ && sep(builder_, level_ + 1);
    result_ = result_ && ifCompoundStatement_6_0_2(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // blockBody?
  private static boolean ifCompoundStatement_6_0_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ifCompoundStatement_6_0_2")) return false;
    blockBody(builder_, level_ + 1);
    return true;
  }

  // [if]
  private static boolean ifCompoundStatement_8(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ifCompoundStatement_8")) return false;
    consumeToken(builder_, IF);
    return true;
  }

  /* ********************************************************** */
  // if expression then (statement|expression)
  public static boolean ifSimpleStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ifSimpleStatement")) return false;
    if (!nextTokenIs(builder_, IF)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, IF);
    result_ = result_ && expression(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, THEN);
    result_ = result_ && ifSimpleStatement_3(builder_, level_ + 1);
    exit_section_(builder_, marker_, IF_SIMPLE_STATEMENT, result_);
    return result_;
  }

  // statement|expression
  private static boolean ifSimpleStatement_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ifSimpleStatement_3")) return false;
    boolean result_;
    result_ = statement(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // ifCompoundStatement|ifSimpleStatement
  static boolean ifStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ifStatement")) return false;
    if (!nextTokenIs(builder_, IF)) return false;
    boolean result_;
    result_ = ifCompoundStatement(builder_, level_ + 1);
    if (!result_) result_ = ifSimpleStatement(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // ignoring considerOrIgnoreAttr ((COMMA considerOrIgnoreAttr)* (LAND considerOrIgnoreAttr)?)?
  //                         [but considering considerOrIgnoreAttr ((COMMA considerOrIgnoreAttr)* (LAND considerOrIgnoreAttr)?)? ] sep
  //                            blockBody?
  //                       end ignoring
  public static boolean ignoringStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ignoringStatement")) return false;
    if (!nextTokenIs(builder_, IGNORING)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, IGNORING_STATEMENT, null);
    result_ = consumeToken(builder_, IGNORING);
    result_ = result_ && considerOrIgnoreAttr(builder_, level_ + 1);
    pinned_ = result_; // pin = 2
    result_ = result_ && report_error_(builder_, ignoringStatement_2(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, ignoringStatement_3(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, sep(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, ignoringStatement_5(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeTokens(builder_, -1, END, IGNORING)) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // ((COMMA considerOrIgnoreAttr)* (LAND considerOrIgnoreAttr)?)?
  private static boolean ignoringStatement_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ignoringStatement_2")) return false;
    ignoringStatement_2_0(builder_, level_ + 1);
    return true;
  }

  // (COMMA considerOrIgnoreAttr)* (LAND considerOrIgnoreAttr)?
  private static boolean ignoringStatement_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ignoringStatement_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = ignoringStatement_2_0_0(builder_, level_ + 1);
    result_ = result_ && ignoringStatement_2_0_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (COMMA considerOrIgnoreAttr)*
  private static boolean ignoringStatement_2_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ignoringStatement_2_0_0")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!ignoringStatement_2_0_0_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "ignoringStatement_2_0_0", pos_)) break;
    }
    return true;
  }

  // COMMA considerOrIgnoreAttr
  private static boolean ignoringStatement_2_0_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ignoringStatement_2_0_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, COMMA);
    result_ = result_ && considerOrIgnoreAttr(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (LAND considerOrIgnoreAttr)?
  private static boolean ignoringStatement_2_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ignoringStatement_2_0_1")) return false;
    ignoringStatement_2_0_1_0(builder_, level_ + 1);
    return true;
  }

  // LAND considerOrIgnoreAttr
  private static boolean ignoringStatement_2_0_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ignoringStatement_2_0_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, LAND);
    result_ = result_ && considerOrIgnoreAttr(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // [but considering considerOrIgnoreAttr ((COMMA considerOrIgnoreAttr)* (LAND considerOrIgnoreAttr)?)? ]
  private static boolean ignoringStatement_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ignoringStatement_3")) return false;
    ignoringStatement_3_0(builder_, level_ + 1);
    return true;
  }

  // but considering considerOrIgnoreAttr ((COMMA considerOrIgnoreAttr)* (LAND considerOrIgnoreAttr)?)?
  private static boolean ignoringStatement_3_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ignoringStatement_3_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokens(builder_, 0, BUT, CONSIDERING);
    result_ = result_ && considerOrIgnoreAttr(builder_, level_ + 1);
    result_ = result_ && ignoringStatement_3_0_3(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // ((COMMA considerOrIgnoreAttr)* (LAND considerOrIgnoreAttr)?)?
  private static boolean ignoringStatement_3_0_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ignoringStatement_3_0_3")) return false;
    ignoringStatement_3_0_3_0(builder_, level_ + 1);
    return true;
  }

  // (COMMA considerOrIgnoreAttr)* (LAND considerOrIgnoreAttr)?
  private static boolean ignoringStatement_3_0_3_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ignoringStatement_3_0_3_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = ignoringStatement_3_0_3_0_0(builder_, level_ + 1);
    result_ = result_ && ignoringStatement_3_0_3_0_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (COMMA considerOrIgnoreAttr)*
  private static boolean ignoringStatement_3_0_3_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ignoringStatement_3_0_3_0_0")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!ignoringStatement_3_0_3_0_0_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "ignoringStatement_3_0_3_0_0", pos_)) break;
    }
    return true;
  }

  // COMMA considerOrIgnoreAttr
  private static boolean ignoringStatement_3_0_3_0_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ignoringStatement_3_0_3_0_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, COMMA);
    result_ = result_ && considerOrIgnoreAttr(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (LAND considerOrIgnoreAttr)?
  private static boolean ignoringStatement_3_0_3_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ignoringStatement_3_0_3_0_1")) return false;
    ignoringStatement_3_0_3_0_1_0(builder_, level_ + 1);
    return true;
  }

  // LAND considerOrIgnoreAttr
  private static boolean ignoringStatement_3_0_3_0_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ignoringStatement_3_0_3_0_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, LAND);
    result_ = result_ && considerOrIgnoreAttr(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // blockBody?
  private static boolean ignoringStatement_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ignoringStatement_5")) return false;
    blockBody(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // <<parseIncompleteCommandCall>>
  public static boolean incompleteCommandHandlerCall(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "incompleteCommandHandlerCall")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, INCOMPLETE_COMMAND_HANDLER_CALL, "<incomplete command handler call>");
    result_ = parseIncompleteCommandCall(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // tell expression | set objectReferenceWrapper | copy expression | if expression
  public static boolean incompleteExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "incompleteExpression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, INCOMPLETE_EXPRESSION, "<incomplete expression>");
    result_ = incompleteExpression_0(builder_, level_ + 1);
    if (!result_) result_ = incompleteExpression_1(builder_, level_ + 1);
    if (!result_) result_ = incompleteExpression_2(builder_, level_ + 1);
    if (!result_) result_ = incompleteExpression_3(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // tell expression
  private static boolean incompleteExpression_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "incompleteExpression_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, TELL);
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // set objectReferenceWrapper
  private static boolean incompleteExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "incompleteExpression_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, SET);
    result_ = result_ && objectReferenceWrapper(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // copy expression
  private static boolean incompleteExpression_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "incompleteExpression_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, COPY);
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // if expression
  private static boolean incompleteExpression_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "incompleteExpression_3")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, IF);
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // (integerLiteralExpression('st'|'rd'|'th') (typeSpecifier|userClassName))
  //                    |((first|second|third|fourth|fifth|sixth|seventh|eighth|ninth|tenth) (typeSpecifier|userClassName) )
  //                    |((last|front|back) (typeSpecifier|userClassName))
  public static boolean indexReference(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "indexReference")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, INDEX_REFERENCE, "<index reference>");
    result_ = indexReference_0(builder_, level_ + 1);
    if (!result_) result_ = indexReference_1(builder_, level_ + 1);
    if (!result_) result_ = indexReference_2(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // integerLiteralExpression('st'|'rd'|'th') (typeSpecifier|userClassName)
  private static boolean indexReference_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "indexReference_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = integerLiteralExpression(builder_, level_ + 1);
    result_ = result_ && indexReference_0_1(builder_, level_ + 1);
    result_ = result_ && indexReference_0_2(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // 'st'|'rd'|'th'
  private static boolean indexReference_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "indexReference_0_1")) return false;
    boolean result_;
    result_ = consumeToken(builder_, "st");
    if (!result_) result_ = consumeToken(builder_, "rd");
    if (!result_) result_ = consumeToken(builder_, "th");
    return result_;
  }

  // typeSpecifier|userClassName
  private static boolean indexReference_0_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "indexReference_0_2")) return false;
    boolean result_;
    result_ = typeSpecifier(builder_, level_ + 1);
    if (!result_) result_ = userClassName(builder_, level_ + 1);
    return result_;
  }

  // (first|second|third|fourth|fifth|sixth|seventh|eighth|ninth|tenth) (typeSpecifier|userClassName)
  private static boolean indexReference_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "indexReference_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = indexReference_1_0(builder_, level_ + 1);
    result_ = result_ && indexReference_1_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // first|second|third|fourth|fifth|sixth|seventh|eighth|ninth|tenth
  private static boolean indexReference_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "indexReference_1_0")) return false;
    boolean result_;
    result_ = consumeToken(builder_, FIRST);
    if (!result_) result_ = consumeToken(builder_, SECOND);
    if (!result_) result_ = consumeToken(builder_, THIRD);
    if (!result_) result_ = consumeToken(builder_, FOURTH);
    if (!result_) result_ = consumeToken(builder_, FIFTH);
    if (!result_) result_ = consumeToken(builder_, SIXTH);
    if (!result_) result_ = consumeToken(builder_, SEVENTH);
    if (!result_) result_ = consumeToken(builder_, EIGHTH);
    if (!result_) result_ = consumeToken(builder_, NINTH);
    if (!result_) result_ = consumeToken(builder_, TENTH);
    return result_;
  }

  // typeSpecifier|userClassName
  private static boolean indexReference_1_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "indexReference_1_1")) return false;
    boolean result_;
    result_ = typeSpecifier(builder_, level_ + 1);
    if (!result_) result_ = userClassName(builder_, level_ + 1);
    return result_;
  }

  // (last|front|back) (typeSpecifier|userClassName)
  private static boolean indexReference_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "indexReference_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = indexReference_2_0(builder_, level_ + 1);
    result_ = result_ && indexReference_2_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // last|front|back
  private static boolean indexReference_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "indexReference_2_0")) return false;
    boolean result_;
    result_ = consumeToken(builder_, LAST);
    if (!result_) result_ = consumeToken(builder_, FRONT);
    if (!result_) result_ = consumeToken(builder_, BACK);
    return result_;
  }

  // typeSpecifier|userClassName
  private static boolean indexReference_2_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "indexReference_2_1")) return false;
    boolean result_;
    result_ = typeSpecifier(builder_, level_ + 1);
    if (!result_) result_ = userClassName(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // [index] indexValueExpression
  public static boolean indexReferenceClassForm(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "indexReferenceClassForm")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, INDEX_REFERENCE_CLASS_FORM, "<index reference class form>");
    result_ = indexReferenceClassForm_0(builder_, level_ + 1);
    result_ = result_ && indexValueExpression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // [index]
  private static boolean indexReferenceClassForm_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "indexReferenceClassForm_0")) return false;
    consumeToken(builder_, INDEX);
    return true;
  }

  /* ********************************************************** */
  // referenceExpression|integerLiteralExpression|parenthesizedExpression
  static boolean indexValueExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "indexValueExpression")) return false;
    boolean result_;
    result_ = referenceExpression(builder_, level_ + 1);
    if (!result_) result_ = integerLiteralExpression(builder_, level_ + 1);
    if (!result_) result_ = parenthesizedExpression(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // DIGITS
  public static boolean integerLiteralExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "integerLiteralExpression")) return false;
    if (!nextTokenIsFast(builder_, DIGITS)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, DIGITS);
    exit_section_(builder_, marker_, INTEGER_LITERAL_EXPRESSION, result_);
    return result_;
  }

  /* ********************************************************** */
  // it|me|ITS
  static boolean itMeProperty(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "itMeProperty")) return false;
    boolean result_;
    result_ = consumeToken(builder_, IT);
    if (!result_) result_ = consumeToken(builder_, ME);
    if (!result_) result_ = consumeToken(builder_, ITS);
    return result_;
  }

  /* ********************************************************** */
  // expression
  static boolean labelForFalseParam(PsiBuilder builder_, int level_) {
    return expression(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // expression
  static boolean labelForTrueParam(PsiBuilder builder_, int level_) {
    return expression(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // ((of|in)? directParameterDeclaration | targetVariablePattern )? labeledParameterDeclarationPart*
  public static boolean labeledParameterDeclarationList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "labeledParameterDeclarationList")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, LABELED_PARAMETER_DECLARATION_LIST, "<labeled parameter declaration list>");
    result_ = labeledParameterDeclarationList_0(builder_, level_ + 1);
    result_ = result_ && labeledParameterDeclarationList_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // ((of|in)? directParameterDeclaration | targetVariablePattern )?
  private static boolean labeledParameterDeclarationList_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "labeledParameterDeclarationList_0")) return false;
    labeledParameterDeclarationList_0_0(builder_, level_ + 1);
    return true;
  }

  // (of|in)? directParameterDeclaration | targetVariablePattern
  private static boolean labeledParameterDeclarationList_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "labeledParameterDeclarationList_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = labeledParameterDeclarationList_0_0_0(builder_, level_ + 1);
    if (!result_) result_ = targetVariablePattern(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (of|in)? directParameterDeclaration
  private static boolean labeledParameterDeclarationList_0_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "labeledParameterDeclarationList_0_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = labeledParameterDeclarationList_0_0_0_0(builder_, level_ + 1);
    result_ = result_ && directParameterDeclaration(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (of|in)?
  private static boolean labeledParameterDeclarationList_0_0_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "labeledParameterDeclarationList_0_0_0_0")) return false;
    labeledParameterDeclarationList_0_0_0_0_0(builder_, level_ + 1);
    return true;
  }

  // of|in
  private static boolean labeledParameterDeclarationList_0_0_0_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "labeledParameterDeclarationList_0_0_0_0_0")) return false;
    boolean result_;
    result_ = consumeToken(builder_, OF);
    if (!result_) result_ = consumeToken(builder_, IN);
    return result_;
  }

  // labeledParameterDeclarationPart*
  private static boolean labeledParameterDeclarationList_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "labeledParameterDeclarationList_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!labeledParameterDeclarationPart(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "labeledParameterDeclarationList_1", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // handlerParameterLabel THE_KW? identifier
  public static boolean labeledParameterDeclarationPart(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "labeledParameterDeclarationPart")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, LABELED_PARAMETER_DECLARATION_PART, "<labeled parameter declaration part>");
    result_ = handlerParameterLabel(builder_, level_ + 1);
    result_ = result_ && labeledParameterDeclarationPart_1(builder_, level_ + 1);
    result_ = result_ && identifier(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // THE_KW?
  private static boolean labeledParameterDeclarationPart_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "labeledParameterDeclarationPart_1")) return false;
    consumeToken(builder_, THE_KW);
    return true;
  }

  /* ********************************************************** */
  // launch expression?
  public static boolean launchCommandExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "launchCommandExpression")) return false;
    if (!nextTokenIsFast(builder_, LAUNCH)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, LAUNCH);
    result_ = result_ && launchCommandExpression_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, LAUNCH_COMMAND_EXPRESSION, result_);
    return result_;
  }

  // expression?
  private static boolean launchCommandExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "launchCommandExpression_1")) return false;
    expression(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // LCURLY [formalParameterListPartPattern|expression] (COMMA formalParameterListPartPattern|expression)* RCURLY
  public static boolean listFormalParameter(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "listFormalParameter")) return false;
    if (!nextTokenIs(builder_, LCURLY)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, LCURLY);
    result_ = result_ && listFormalParameter_1(builder_, level_ + 1);
    result_ = result_ && listFormalParameter_2(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, RCURLY);
    exit_section_(builder_, marker_, LIST_FORMAL_PARAMETER, result_);
    return result_;
  }

  // [formalParameterListPartPattern|expression]
  private static boolean listFormalParameter_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "listFormalParameter_1")) return false;
    listFormalParameter_1_0(builder_, level_ + 1);
    return true;
  }

  // formalParameterListPartPattern|expression
  private static boolean listFormalParameter_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "listFormalParameter_1_0")) return false;
    boolean result_;
    result_ = formalParameterListPartPattern(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    return result_;
  }

  // (COMMA formalParameterListPartPattern|expression)*
  private static boolean listFormalParameter_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "listFormalParameter_2")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!listFormalParameter_2_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "listFormalParameter_2", pos_)) break;
    }
    return true;
  }

  // COMMA formalParameterListPartPattern|expression
  private static boolean listFormalParameter_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "listFormalParameter_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = listFormalParameter_2_0_0(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // COMMA formalParameterListPartPattern
  private static boolean listFormalParameter_2_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "listFormalParameter_2_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, COMMA);
    result_ = result_ && formalParameterListPartPattern(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // LCURLY [expression] (COMMA expression)* RCURLY
  public static boolean listLiteralExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "listLiteralExpression")) return false;
    if (!nextTokenIsFast(builder_, LCURLY)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, LCURLY);
    result_ = result_ && listLiteralExpression_1(builder_, level_ + 1);
    result_ = result_ && listLiteralExpression_2(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, RCURLY);
    exit_section_(builder_, marker_, LIST_LITERAL_EXPRESSION, result_);
    return result_;
  }

  // [expression]
  private static boolean listLiteralExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "listLiteralExpression_1")) return false;
    expression(builder_, level_ + 1);
    return true;
  }

  // (COMMA expression)*
  private static boolean listLiteralExpression_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "listLiteralExpression_2")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!listLiteralExpression_2_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "listLiteralExpression_2", pos_)) break;
    }
    return true;
  }

  // COMMA expression
  private static boolean listLiteralExpression_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "listLiteralExpression_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, COMMA);
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // expression
  static boolean listOrReferenceExpression(PsiBuilder builder_, int level_) {
    return expression(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // stringLiteralExpression
  //               |styledTextLiteralExpression
  //               |numberLiteralExpression
  //               |dictionaryConstant
  //               |builtInConstantLiteralExpression
  //               |<<parseLiteralExpression listLiteralExpression>>
  //               |<<parseLiteralExpression recordLiteralExpression>>
  //               |<<parseExpression '"date"' dateLiteralExpression>>
  //               |aReferenceToLiteralExpression
  static boolean literalExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "literalExpression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = stringLiteralExpression(builder_, level_ + 1);
    if (!result_) result_ = styledTextLiteralExpression(builder_, level_ + 1);
    if (!result_) result_ = numberLiteralExpression(builder_, level_ + 1);
    if (!result_) result_ = dictionaryConstant(builder_, level_ + 1);
    if (!result_) result_ = builtInConstantLiteralExpression(builder_, level_ + 1);
    if (!result_) result_ = parseLiteralExpression(builder_, level_ + 1, AppleScriptParser::listLiteralExpression);
    if (!result_) result_ = parseLiteralExpression(builder_, level_ + 1, AppleScriptParser::recordLiteralExpression);
    if (!result_) result_ = parseExpression(builder_, level_ + 1, "date", AppleScriptParser::dateLiteralExpression);
    if (!result_) result_ = aReferenceToLiteralExpression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // log expression
  public static boolean logCommandExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "logCommandExpression")) return false;
    if (!nextTokenIsFast(builder_, LOG)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, LOG);
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, LOG_COMMAND_EXPRESSION, result_);
    return result_;
  }

  /* ********************************************************** */
  // LAND NLS* negationExpressionWrapper
  public static boolean logicalAndExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "logicalAndExpression")) return false;
    if (!nextTokenIsFast(builder_, LAND)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, LOGICAL_AND_EXPRESSION, null);
    result_ = consumeTokenFast(builder_, LAND);
    result_ = result_ && logicalAndExpression_1(builder_, level_ + 1);
    result_ = result_ && negationExpressionWrapper(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // NLS*
  private static boolean logicalAndExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "logicalAndExpression_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!consumeTokenFast(builder_, NLS)) break;
      if (!empty_element_parsed_guard_(builder_, "logicalAndExpression_1", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // negationExpressionWrapper logicalAndExpression*
  static boolean logicalAndExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "logicalAndExpressionWrapper")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = negationExpressionWrapper(builder_, level_ + 1);
    result_ = result_ && logicalAndExpressionWrapper_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // logicalAndExpression*
  private static boolean logicalAndExpressionWrapper_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "logicalAndExpressionWrapper_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!logicalAndExpression(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "logicalAndExpressionWrapper_1", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // LOR NLS* logicalAndExpressionWrapper
  public static boolean logicalOrExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "logicalOrExpression")) return false;
    if (!nextTokenIsFast(builder_, LOR)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, LOGICAL_OR_EXPRESSION, null);
    result_ = consumeTokenFast(builder_, LOR);
    result_ = result_ && logicalOrExpression_1(builder_, level_ + 1);
    result_ = result_ && logicalAndExpressionWrapper(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // NLS*
  private static boolean logicalOrExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "logicalOrExpression_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!consumeTokenFast(builder_, NLS)) break;
      if (!empty_element_parsed_guard_(builder_, "logicalOrExpression_1", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // logicalAndExpressionWrapper logicalOrExpression*
  static boolean logicalOrExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "logicalOrExpressionWrapper")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = logicalAndExpressionWrapper(builder_, level_ + 1);
    result_ = result_ && logicalOrExpressionWrapper_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // logicalOrExpression*
  private static boolean logicalOrExpressionWrapper_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "logicalOrExpressionWrapper_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!logicalOrExpression(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "logicalOrExpressionWrapper_1", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // directParameterDeclaration
  static boolean loopVariable(PsiBuilder builder_, int level_) {
    return directParameterDeclaration(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // STRING_LITERAL
  static boolean machineName(PsiBuilder builder_, int level_) {
    return consumeToken(builder_, STRING_LITERAL);
  }

  /* ********************************************************** */
  // middle typeSpecifier
  public static boolean middleElemReference(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "middleElemReference")) return false;
    if (!nextTokenIs(builder_, MIDDLE)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, MIDDLE_ELEM_REFERENCE, null);
    result_ = consumeToken(builder_, MIDDLE);
    pinned_ = result_; // pin = 1
    result_ = result_ && typeSpecifier(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // MISSING_VALUE
  static boolean missing_value_constant(PsiBuilder builder_, int level_) {
    return consumeToken(builder_, MISSING_VALUE);
  }

  /* ********************************************************** */
  // 'January'|'February'|'March'|'April'|'May'|'June'|'July'|'August'|'September'|'October'
  //                     |'November'|'December' |'Jan'|'Feb'|'Mar'|'Apr'|'Jun'|'Jul'|'Aug'|'Sep'|'Oct'|'Nov'|'Dec'
  static boolean month_constant(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "month_constant")) return false;
    boolean result_;
    result_ = consumeToken(builder_, "January");
    if (!result_) result_ = consumeToken(builder_, "February");
    if (!result_) result_ = consumeToken(builder_, "March");
    if (!result_) result_ = consumeToken(builder_, "April");
    if (!result_) result_ = consumeToken(builder_, "May");
    if (!result_) result_ = consumeToken(builder_, "June");
    if (!result_) result_ = consumeToken(builder_, "July");
    if (!result_) result_ = consumeToken(builder_, "August");
    if (!result_) result_ = consumeToken(builder_, "September");
    if (!result_) result_ = consumeToken(builder_, "October");
    if (!result_) result_ = consumeToken(builder_, "November");
    if (!result_) result_ = consumeToken(builder_, "December");
    if (!result_) result_ = consumeToken(builder_, "Jan");
    if (!result_) result_ = consumeToken(builder_, "Feb");
    if (!result_) result_ = consumeToken(builder_, "Mar");
    if (!result_) result_ = consumeToken(builder_, "Apr");
    if (!result_) result_ = consumeToken(builder_, "Jun");
    if (!result_) result_ = consumeToken(builder_, "Jul");
    if (!result_) result_ = consumeToken(builder_, "Aug");
    if (!result_) result_ = consumeToken(builder_, "Sep");
    if (!result_) result_ = consumeToken(builder_, "Oct");
    if (!result_) result_ = consumeToken(builder_, "Nov");
    if (!result_) result_ = consumeToken(builder_, "Dec");
    return result_;
  }

  /* ********************************************************** */
  // multiplicativeOperator NLS* powerExpressionWrapper
  public static boolean multiplicativeExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "multiplicativeExpression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, MULTIPLICATIVE_EXPRESSION, "<multiplicative expression>");
    result_ = multiplicativeOperator(builder_, level_ + 1);
    result_ = result_ && multiplicativeExpression_1(builder_, level_ + 1);
    result_ = result_ && powerExpressionWrapper(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // NLS*
  private static boolean multiplicativeExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "multiplicativeExpression_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!consumeTokenFast(builder_, NLS)) break;
      if (!empty_element_parsed_guard_(builder_, "multiplicativeExpression_1", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // powerExpressionWrapper multiplicativeExpression*
  static boolean multiplicativeExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "multiplicativeExpressionWrapper")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = powerExpressionWrapper(builder_, level_ + 1);
    result_ = result_ && multiplicativeExpressionWrapper_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // multiplicativeExpression*
  private static boolean multiplicativeExpressionWrapper_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "multiplicativeExpressionWrapper_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!multiplicativeExpression(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "multiplicativeExpressionWrapper_1", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // STAR|DIV|INT_DIV|MOD
  static boolean multiplicativeOperator(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "multiplicativeOperator")) return false;
    boolean result_;
    result_ = consumeToken(builder_, STAR);
    if (!result_) result_ = consumeToken(builder_, DIV);
    if (!result_) result_ = consumeToken(builder_, INT_DIV);
    if (!result_) result_ = consumeToken(builder_, MOD);
    return result_;
  }

  /* ********************************************************** */
  // nameReferenceString
  public static boolean nameReference(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "nameReference")) return false;
    if (!nextTokenIsFast(builder_, STRING_LITERAL) &&
        !nextTokenIs(builder_, "<name reference>", NAMED)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, NAME_REFERENCE, "<name reference>");
    result_ = nameReferenceString(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // [named] nameStringExpression
  static boolean nameReferenceString(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "nameReferenceString")) return false;
    if (!nextTokenIsFast(builder_, STRING_LITERAL) &&
        !nextTokenIs(builder_, "", NAMED)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = nameReferenceString_0(builder_, level_ + 1);
    result_ = result_ && nameStringExpression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // [named]
  private static boolean nameReferenceString_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "nameReferenceString_0")) return false;
    consumeToken(builder_, NAMED);
    return true;
  }

  /* ********************************************************** */
  // stringLiteralExpression
  static boolean nameStringExpression(PsiBuilder builder_, int level_) {
    return stringLiteralExpression(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // LNOT NLS* negationExpressionWrapper
  public static boolean negationExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "negationExpression")) return false;
    if (!nextTokenIsFast(builder_, LNOT)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, LNOT);
    result_ = result_ && negationExpression_1(builder_, level_ + 1);
    result_ = result_ && negationExpressionWrapper(builder_, level_ + 1);
    exit_section_(builder_, marker_, NEGATION_EXPRESSION, result_);
    return result_;
  }

  // NLS*
  private static boolean negationExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "negationExpression_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!consumeTokenFast(builder_, NLS)) break;
      if (!empty_element_parsed_guard_(builder_, "negationExpression_1", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // negationExpression | compareExpressionWrapper
  static boolean negationExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "negationExpressionWrapper")) return false;
    boolean result_;
    result_ = negationExpression(builder_, level_ + 1);
    if (!result_) result_ = compareExpressionWrapper(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // expression
  static boolean numTimes(PsiBuilder builder_, int level_) {
    return expression(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // realLiteralExpression|integerLiteralExpression|numericConstant
  public static boolean numberLiteralExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "numberLiteralExpression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _COLLAPSE_, NUMBER_LITERAL_EXPRESSION, "<number literal expression>");
    result_ = realLiteralExpression(builder_, level_ + 1);
    if (!result_) result_ = integerLiteralExpression(builder_, level_ + 1);
    if (!result_) result_ = numericConstant(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // PI_CONSTANT|seconds_constants
  public static boolean numericConstant(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "numericConstant")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, NUMERIC_CONSTANT, "<numeric constant>");
    result_ = consumeToken(builder_, PI_CONSTANT);
    if (!result_) result_ = seconds_constants(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // propertyLabelIdentifier COLON (formalParameterListPartPattern|propertyInitializerExpression)
  public static boolean objectNamedPropertyDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "objectNamedPropertyDeclaration")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, OBJECT_NAMED_PROPERTY_DECLARATION, "<object named property declaration>");
    result_ = propertyLabelIdentifier(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, COLON);
    result_ = result_ && objectNamedPropertyDeclaration_2(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // formalParameterListPartPattern|propertyInitializerExpression
  private static boolean objectNamedPropertyDeclaration_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "objectNamedPropertyDeclaration_2")) return false;
    boolean result_;
    result_ = formalParameterListPartPattern(builder_, level_ + 1);
    if (!result_) result_ = propertyInitializerExpression(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // of|in|APS|<<isPossessivePpronoun>>
  static boolean objectPointer(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "objectPointer")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, OF);
    if (!result_) result_ = consumeToken(builder_, IN);
    if (!result_) result_ = consumeToken(builder_, APS);
    if (!result_) result_ = isPossessivePpronoun(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // propertyLabelIdentifier COLON propertyInitializerExpression
  public static boolean objectPropertyDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "objectPropertyDeclaration")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, OBJECT_PROPERTY_DECLARATION, "<object property declaration>");
    result_ = propertyLabelIdentifier(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, COLON);
    result_ = result_ && propertyInitializerExpression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // possessiveFormAndInterleavedCall | objectPointer THE_KW? prefixExpression
  public static boolean objectReferenceExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "objectReferenceExpression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, OBJECT_REFERENCE_EXPRESSION, "<object reference expression>");
    result_ = possessiveFormAndInterleavedCall(builder_, level_ + 1);
    if (!result_) result_ = objectReferenceExpression_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // objectPointer THE_KW? prefixExpression
  private static boolean objectReferenceExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "objectReferenceExpression_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = objectPointer(builder_, level_ + 1);
    result_ = result_ && objectReferenceExpression_1_1(builder_, level_ + 1);
    result_ = result_ && prefixExpression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // THE_KW?
  private static boolean objectReferenceExpression_1_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "objectReferenceExpression_1_1")) return false;
    consumeTokenFast(builder_, THE_KW);
    return true;
  }

  /* ********************************************************** */
  // THE_KW? prefixExpression objectReferenceExpression*
  static boolean objectReferenceWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "objectReferenceWrapper")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = objectReferenceWrapper_0(builder_, level_ + 1);
    result_ = result_ && prefixExpression(builder_, level_ + 1);
    result_ = result_ && objectReferenceWrapper_2(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // THE_KW?
  private static boolean objectReferenceWrapper_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "objectReferenceWrapper_0")) return false;
    consumeToken(builder_, THE_KW);
    return true;
  }

  // objectReferenceExpression*
  private static boolean objectReferenceWrapper_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "objectReferenceWrapper_2")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!objectReferenceExpression(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "objectReferenceWrapper_2", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // propertyLabelIdentifier COLON (targetVariablePattern|propertyInitializerExpression)
  public static boolean objectTargetPropertyDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "objectTargetPropertyDeclaration")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, OBJECT_TARGET_PROPERTY_DECLARATION, "<object target property declaration>");
    result_ = propertyLabelIdentifier(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, COLON);
    result_ = result_ && objectTargetPropertyDeclaration_2(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // targetVariablePattern|propertyInitializerExpression
  private static boolean objectTargetPropertyDeclaration_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "objectTargetPropertyDeclaration_2")) return false;
    boolean result_;
    result_ = targetVariablePattern(builder_, level_ + 1);
    if (!result_) result_ = propertyInitializerExpression(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // (referenceExpression &to) | expression
  static boolean offendingObject(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "offendingObject")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = offendingObject_0(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // referenceExpression &to
  private static boolean offendingObject_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "offendingObject_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = referenceExpression(builder_, level_ + 1);
    result_ = result_ && offendingObject_0_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // &to
  private static boolean offendingObject_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "offendingObject_0_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _AND_);
    result_ = consumeToken(builder_, TO);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // directParameterDeclaration|expression
  static boolean offendingObjectVar(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "offendingObjectVar")) return false;
    boolean result_;
    result_ = directParameterDeclaration(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // referenceIdBeforeParamLabel | expression
  public static boolean parameterVal(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parameterVal")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, PARAMETER_VAL, "<parameter val>");
    result_ = referenceIdBeforeParamLabel(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // expression
  static boolean parameterValue(PsiBuilder builder_, int level_) {
    return expression(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // parent
  static boolean parentProperty(PsiBuilder builder_, int level_) {
    return consumeToken(builder_, PARENT);
  }

  /* ********************************************************** */
  // LPAREN expressionInParentheses RPAREN
  public static boolean parenthesizedExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parenthesizedExpression")) return false;
    if (!nextTokenIsFast(builder_, LPAREN)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, LPAREN);
    result_ = result_ && expressionInParentheses(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, RPAREN);
    exit_section_(builder_, marker_, PARENTHESIZED_EXPRESSION, result_);
    return result_;
  }

  /* ********************************************************** */
  // <<parseAssignmentStatementInner>>
  static boolean parseAssignmentStatement(PsiBuilder builder_, int level_) {
    return parseAssignmentStatementInner(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // <<parseTellSimpleStatementInner>>
  static boolean parseTellSimpleStatement(PsiBuilder builder_, int level_) {
    return parseTellSimpleStatementInner(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // <<parseExpression '"path"' pathToConstantExpressionInner>>
  static boolean pathToConstantExpression(PsiBuilder builder_, int level_) {
    return parseExpression(builder_, level_ + 1, "path", AppleScriptParser::pathToConstantExpressionInner);
  }

  /* ********************************************************** */
  // var_identifier to var_identifier+ (from var_identifier+)?
  static boolean pathToConstantExpressionInner(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "pathToConstantExpressionInner")) return false;
    if (!nextTokenIs(builder_, VAR_IDENTIFIER)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokens(builder_, 0, VAR_IDENTIFIER, TO);
    result_ = result_ && pathToConstantExpressionInner_2(builder_, level_ + 1);
    result_ = result_ && pathToConstantExpressionInner_3(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // var_identifier+
  private static boolean pathToConstantExpressionInner_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "pathToConstantExpressionInner_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, VAR_IDENTIFIER);
    while (result_) {
      int pos_ = current_position_(builder_);
      if (!consumeToken(builder_, VAR_IDENTIFIER)) break;
      if (!empty_element_parsed_guard_(builder_, "pathToConstantExpressionInner_2", pos_)) break;
    }
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (from var_identifier+)?
  private static boolean pathToConstantExpressionInner_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "pathToConstantExpressionInner_3")) return false;
    pathToConstantExpressionInner_3_0(builder_, level_ + 1);
    return true;
  }

  // from var_identifier+
  private static boolean pathToConstantExpressionInner_3_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "pathToConstantExpressionInner_3_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, FROM);
    result_ = result_ && pathToConstantExpressionInner_3_0_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // var_identifier+
  private static boolean pathToConstantExpressionInner_3_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "pathToConstantExpressionInner_3_0_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, VAR_IDENTIFIER);
    while (result_) {
      int pos_ = current_position_(builder_);
      if (!consumeToken(builder_, VAR_IDENTIFIER)) break;
      if (!empty_element_parsed_guard_(builder_, "pathToConstantExpressionInner_3_0_1", pos_)) break;
    }
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // builtInClassIdentifierPlural | dictionaryClassIdentifierPlural | rawClassExpression
  static boolean pluralClassName(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "pluralClassName")) return false;
    boolean result_;
    result_ = builtInClassIdentifierPlural(builder_, level_ + 1);
    if (!result_) result_ = dictionaryClassIdentifierPlural(builder_, level_ + 1);
    if (!result_) result_ = rawClassExpression(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // (APS|<<isPossessivePpronoun>>) handlerInterleavedParametersCall
  static boolean possessiveFormAndInterleavedCall(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "possessiveFormAndInterleavedCall")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = possessiveFormAndInterleavedCall_0(builder_, level_ + 1);
    result_ = result_ && handlerInterleavedParametersCall(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // APS|<<isPossessivePpronoun>>
  private static boolean possessiveFormAndInterleavedCall_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "possessiveFormAndInterleavedCall_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, APS);
    if (!result_) result_ = isPossessivePpronoun(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // POW objectReferenceWrapper
  public static boolean powerExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "powerExpression")) return false;
    if (!nextTokenIsFast(builder_, POW)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, POWER_EXPRESSION, null);
    result_ = consumeTokenFast(builder_, POW);
    result_ = result_ && objectReferenceWrapper(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // objectReferenceWrapper powerExpression*
  static boolean powerExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "powerExpressionWrapper")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = objectReferenceWrapper(builder_, level_ + 1);
    result_ = result_ && powerExpressionWrapper_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // powerExpression*
  private static boolean powerExpressionWrapper_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "powerExpressionWrapper_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!powerExpression(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "powerExpressionWrapper_1", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // (prefixOperator prefixExpression) | valueExpression
  static boolean prefixExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "prefixExpression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = prefixExpression_0(builder_, level_ + 1);
    if (!result_) result_ = valueExpression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // prefixOperator prefixExpression
  private static boolean prefixExpression_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "prefixExpression_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = prefixOperator(builder_, level_ + 1);
    result_ = result_ && prefixExpression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // PLUS|MINUS
  static boolean prefixOperator(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "prefixOperator")) return false;
    if (!nextTokenIs(builder_, "", MINUS, PLUS)) return false;
    boolean result_;
    result_ = consumeToken(builder_, PLUS);
    if (!result_) result_ = consumeToken(builder_, MINUS);
    return result_;
  }

  /* ********************************************************** */
  // literalExpression
  //  |   dictionaryCommandHandlerCallExpression
  //  |   asciiObjectExpression
  //  |   pathToConstantExpression
  //  |   rawDictionaryCommandHandlerCallExpression
  //  |   rawDataExpression
  //  | ( primaryReferenceExpression
  //      (
  //      <<isTreePrevSimpleReference>>
  //        (  handlerPositionalParametersCallExpression | <<isHandlerLabeledParametersCallAllowed>>  handlerLabeledParametersCallExpression )
  //      )?
  //    )
  //  |   parenthesizedExpression
  //  |   appleScriptCommandExpression
  static boolean primaryExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "primaryExpression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = literalExpression(builder_, level_ + 1);
    if (!result_) result_ = dictionaryCommandHandlerCallExpression(builder_, level_ + 1);
    if (!result_) result_ = asciiObjectExpression(builder_, level_ + 1);
    if (!result_) result_ = pathToConstantExpression(builder_, level_ + 1);
    if (!result_) result_ = rawDictionaryCommandHandlerCallExpression(builder_, level_ + 1);
    if (!result_) result_ = rawDataExpression(builder_, level_ + 1);
    if (!result_) result_ = primaryExpression_6(builder_, level_ + 1);
    if (!result_) result_ = parenthesizedExpression(builder_, level_ + 1);
    if (!result_) result_ = appleScriptCommandExpression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // primaryReferenceExpression
  //      (
  //      <<isTreePrevSimpleReference>>
  //        (  handlerPositionalParametersCallExpression | <<isHandlerLabeledParametersCallAllowed>>  handlerLabeledParametersCallExpression )
  //      )?
  private static boolean primaryExpression_6(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "primaryExpression_6")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = primaryReferenceExpression(builder_, level_ + 1);
    result_ = result_ && primaryExpression_6_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (
  //      <<isTreePrevSimpleReference>>
  //        (  handlerPositionalParametersCallExpression | <<isHandlerLabeledParametersCallAllowed>>  handlerLabeledParametersCallExpression )
  //      )?
  private static boolean primaryExpression_6_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "primaryExpression_6_1")) return false;
    primaryExpression_6_1_0(builder_, level_ + 1);
    return true;
  }

  // <<isTreePrevSimpleReference>>
  //        (  handlerPositionalParametersCallExpression | <<isHandlerLabeledParametersCallAllowed>>  handlerLabeledParametersCallExpression )
  private static boolean primaryExpression_6_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "primaryExpression_6_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = isTreePrevSimpleReference(builder_, level_ + 1);
    result_ = result_ && primaryExpression_6_1_0_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // handlerPositionalParametersCallExpression | <<isHandlerLabeledParametersCallAllowed>>  handlerLabeledParametersCallExpression
  private static boolean primaryExpression_6_1_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "primaryExpression_6_1_0_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = handlerPositionalParametersCallExpression(builder_, level_ + 1);
    if (!result_) result_ = primaryExpression_6_1_0_1_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // <<isHandlerLabeledParametersCallAllowed>>  handlerLabeledParametersCallExpression
  private static boolean primaryExpression_6_1_0_1_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "primaryExpression_6_1_0_1_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = isHandlerLabeledParametersCallAllowed(builder_, level_ + 1);
    result_ = result_ && handlerLabeledParametersCallExpression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // applicationReference | referenceForm | referenceExpression
  static boolean primaryReferenceExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "primaryReferenceExpression")) return false;
    boolean result_;
    result_ = applicationReference(builder_, level_ + 1);
    if (!result_) result_ = referenceForm(builder_, level_ + 1);
    if (!result_) result_ = referenceExpression(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // expression
  static boolean propertyInitializerExpression(PsiBuilder builder_, int level_) {
    return expression(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // propertyReference|identifier
  static boolean propertyLabelIdentifier(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "propertyLabelIdentifier")) return false;
    boolean result_;
    result_ = propertyReference(builder_, level_ + 1);
    if (!result_) result_ = identifier(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // dictionaryPropertyName|literalExpression|appleScriptProperty|builtInClassProperty
  public static boolean propertyReference(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "propertyReference")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, PROPERTY_REFERENCE, "<property reference>");
    result_ = dictionaryPropertyName(builder_, level_ + 1);
    if (!result_) result_ = literalExpression(builder_, level_ + 1);
    if (!result_) result_ = appleScriptProperty(builder_, level_ + 1);
    if (!result_) result_ = builtInClassProperty(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // from [THE_KW] (beginning|expression) to [THE_KW](end|expression)
  public static boolean rangeFromReference(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "rangeFromReference")) return false;
    if (!nextTokenIs(builder_, FROM)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, FROM);
    result_ = result_ && rangeFromReference_1(builder_, level_ + 1);
    result_ = result_ && rangeFromReference_2(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, TO);
    result_ = result_ && rangeFromReference_4(builder_, level_ + 1);
    result_ = result_ && rangeFromReference_5(builder_, level_ + 1);
    exit_section_(builder_, marker_, RANGE_FROM_REFERENCE, result_);
    return result_;
  }

  // [THE_KW]
  private static boolean rangeFromReference_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "rangeFromReference_1")) return false;
    consumeToken(builder_, THE_KW);
    return true;
  }

  // beginning|expression
  private static boolean rangeFromReference_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "rangeFromReference_2")) return false;
    boolean result_;
    result_ = consumeToken(builder_, BEGINNING);
    if (!result_) result_ = expression(builder_, level_ + 1);
    return result_;
  }

  // [THE_KW]
  private static boolean rangeFromReference_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "rangeFromReference_4")) return false;
    consumeToken(builder_, THE_KW);
    return true;
  }

  // end|expression
  private static boolean rangeFromReference_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "rangeFromReference_5")) return false;
    boolean result_;
    result_ = consumeToken(builder_, END);
    if (!result_) result_ = expression(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // startIndex (thru|through) stopIndex
  public static boolean rangeIndexReference(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "rangeIndexReference")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, RANGE_INDEX_REFERENCE, "<range index reference>");
    result_ = startIndex(builder_, level_ + 1);
    result_ = result_ && rangeIndexReference_1(builder_, level_ + 1);
    result_ = result_ && stopIndex(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // thru|through
  private static boolean rangeIndexReference_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "rangeIndexReference_1")) return false;
    boolean result_;
    result_ = consumeToken(builder_, THRU);
    if (!result_) result_ = consumeToken(builder_, THROUGH);
    return result_;
  }

  /* ********************************************************** */
  // rangeFromReference | rangeIndexReference
  static boolean rangeReferenceFormWithClassPrefix(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "rangeReferenceFormWithClassPrefix")) return false;
    boolean result_;
    result_ = rangeFromReference(builder_, level_ + 1);
    if (!result_) result_ = rangeIndexReference(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // RAW_LBR CLASS identifier RAW_RBR
  public static boolean rawClassExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "rawClassExpression")) return false;
    if (!nextTokenIsFast(builder_, RAW_LBR)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokens(builder_, 0, RAW_LBR, CLASS);
    result_ = result_ && identifier(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, RAW_RBR);
    exit_section_(builder_, marker_, RAW_CLASS_EXPRESSION, result_);
    return result_;
  }

  /* ********************************************************** */
  // RAW_LBR DATA DIGITS RAW_RBR
  public static boolean rawDataExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "rawDataExpression")) return false;
    if (!nextTokenIsFast(builder_, RAW_LBR)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokens(builder_, 0, RAW_LBR, DATA, DIGITS, RAW_RBR);
    exit_section_(builder_, marker_, RAW_DATA_EXPRESSION, result_);
    return result_;
  }

  /* ********************************************************** */
  // RAW_LBR EVENT identifier RAW_RBR commandRawParameters?
  public static boolean rawDictionaryCommandHandlerCallExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "rawDictionaryCommandHandlerCallExpression")) return false;
    if (!nextTokenIsFast(builder_, RAW_LBR)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokens(builder_, 0, RAW_LBR, EVENT);
    result_ = result_ && identifier(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, RAW_RBR);
    result_ = result_ && rawDictionaryCommandHandlerCallExpression_4(builder_, level_ + 1);
    exit_section_(builder_, marker_, RAW_DICTIONARY_COMMAND_HANDLER_CALL_EXPRESSION, result_);
    return result_;
  }

  // commandRawParameters?
  private static boolean rawDictionaryCommandHandlerCallExpression_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "rawDictionaryCommandHandlerCallExpression_4")) return false;
    commandRawParameters(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // rawParameterSelector expression
  public static boolean rawParameterExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "rawParameterExpression")) return false;
    if (!nextTokenIsFast(builder_, RAW_LBR)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = rawParameterSelector(builder_, level_ + 1);
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, RAW_PARAMETER_EXPRESSION, result_);
    return result_;
  }

  /* ********************************************************** */
  // RAW_LBR PREPOSITION identifier RAW_RBR
  public static boolean rawParameterSelector(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "rawParameterSelector")) return false;
    if (!nextTokenIs(builder_, RAW_LBR)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokens(builder_, 0, RAW_LBR, PREPOSITION);
    result_ = result_ && identifier(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, RAW_RBR);
    exit_section_(builder_, marker_, RAW_PARAMETER_SELECTOR, result_);
    return result_;
  }

  /* ********************************************************** */
  // ('.'(DIGITS)(DEC_EXPONENT))|(dec_significand DEC_EXPONENT?)
  public static boolean realLiteralExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "realLiteralExpression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, REAL_LITERAL_EXPRESSION, "<real literal expression>");
    result_ = realLiteralExpression_0(builder_, level_ + 1);
    if (!result_) result_ = realLiteralExpression_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // '.'(DIGITS)(DEC_EXPONENT)
  private static boolean realLiteralExpression_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "realLiteralExpression_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, ".");
    result_ = result_ && realLiteralExpression_0_1(builder_, level_ + 1);
    result_ = result_ && realLiteralExpression_0_2(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (DIGITS)
  private static boolean realLiteralExpression_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "realLiteralExpression_0_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, DIGITS);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (DEC_EXPONENT)
  private static boolean realLiteralExpression_0_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "realLiteralExpression_0_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, DEC_EXPONENT);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // dec_significand DEC_EXPONENT?
  private static boolean realLiteralExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "realLiteralExpression_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = dec_significand(builder_, level_ + 1);
    result_ = result_ && realLiteralExpression_1_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // DEC_EXPONENT?
  private static boolean realLiteralExpression_1_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "realLiteralExpression_1_1")) return false;
    consumeTokenFast(builder_, DEC_EXPONENT);
    return true;
  }

  /* ********************************************************** */
  // LCURLY[objectNamedPropertyDeclaration] (COMMA objectNamedPropertyDeclaration)* RCURLY
  public static boolean recordFormalParameter(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "recordFormalParameter")) return false;
    if (!nextTokenIs(builder_, LCURLY)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, LCURLY);
    result_ = result_ && recordFormalParameter_1(builder_, level_ + 1);
    result_ = result_ && recordFormalParameter_2(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, RCURLY);
    exit_section_(builder_, marker_, RECORD_FORMAL_PARAMETER, result_);
    return result_;
  }

  // [objectNamedPropertyDeclaration]
  private static boolean recordFormalParameter_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "recordFormalParameter_1")) return false;
    objectNamedPropertyDeclaration(builder_, level_ + 1);
    return true;
  }

  // (COMMA objectNamedPropertyDeclaration)*
  private static boolean recordFormalParameter_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "recordFormalParameter_2")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!recordFormalParameter_2_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "recordFormalParameter_2", pos_)) break;
    }
    return true;
  }

  // COMMA objectNamedPropertyDeclaration
  private static boolean recordFormalParameter_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "recordFormalParameter_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, COMMA);
    result_ = result_ && objectNamedPropertyDeclaration(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // LCURLY[objectPropertyDeclaration] (COMMA objectPropertyDeclaration)* RCURLY
  public static boolean recordLiteralExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "recordLiteralExpression")) return false;
    if (!nextTokenIsFast(builder_, LCURLY)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, LCURLY);
    result_ = result_ && recordLiteralExpression_1(builder_, level_ + 1);
    result_ = result_ && recordLiteralExpression_2(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, RCURLY);
    exit_section_(builder_, marker_, RECORD_LITERAL_EXPRESSION, result_);
    return result_;
  }

  // [objectPropertyDeclaration]
  private static boolean recordLiteralExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "recordLiteralExpression_1")) return false;
    objectPropertyDeclaration(builder_, level_ + 1);
    return true;
  }

  // (COMMA objectPropertyDeclaration)*
  private static boolean recordLiteralExpression_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "recordLiteralExpression_2")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!recordLiteralExpression_2_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "recordLiteralExpression_2", pos_)) break;
    }
    return true;
  }

  // COMMA objectPropertyDeclaration
  private static boolean recordLiteralExpression_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "recordLiteralExpression_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, COMMA);
    result_ = result_ && objectPropertyDeclaration(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // identifier
  public static boolean referenceExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "referenceExpression")) return false;
    if (!nextTokenIsFast(builder_, VAR_IDENTIFIER)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = identifier(builder_, level_ + 1);
    exit_section_(builder_, marker_, REFERENCE_EXPRESSION, result_);
    return result_;
  }

  /* ********************************************************** */
  // (my handlerInterleavedCallOrPropertyReferenceOrReferenceIdentifier )
  //     | (my?
  //     // it looks like only range reference can be compiled using referenceIdentifier. For other forms valid
  //     // class name is a must
  //        (classNamePrimaryExpression
  //         (  rangeReferenceFormWithClassPrefix
  //          | nameReference
  //          | relativeReference
  //          | indexReferenceClassForm
  //          | idReference
  //         )? //can not remove '?' from here.. as script compiles with just className as a primary expression...
  //        )
  //        |
  //        (  arbitraryReference
  //         | everyElemReference
  //         | everyRangeReference
  //         | middleElemReference
  //         | relativeReference
  //         | indexReference
  //         | propertyReference
  //        )
  //       )
  static boolean referenceForm(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "referenceForm")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = referenceForm_0(builder_, level_ + 1);
    if (!result_) result_ = referenceForm_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // my handlerInterleavedCallOrPropertyReferenceOrReferenceIdentifier
  private static boolean referenceForm_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "referenceForm_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, MY);
    result_ = result_ && handlerInterleavedCallOrPropertyReferenceOrReferenceIdentifier(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // my?
  //     // it looks like only range reference can be compiled using referenceIdentifier. For other forms valid
  //     // class name is a must
  //        (classNamePrimaryExpression
  //         (  rangeReferenceFormWithClassPrefix
  //          | nameReference
  //          | relativeReference
  //          | indexReferenceClassForm
  //          | idReference
  //         )? //can not remove '?' from here.. as script compiles with just className as a primary expression...
  //        )
  //        |
  //        (  arbitraryReference
  //         | everyElemReference
  //         | everyRangeReference
  //         | middleElemReference
  //         | relativeReference
  //         | indexReference
  //         | propertyReference
  //        )
  private static boolean referenceForm_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "referenceForm_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = referenceForm_1_0(builder_, level_ + 1);
    if (!result_) result_ = referenceForm_1_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // my?
  //     // it looks like only range reference can be compiled using referenceIdentifier. For other forms valid
  //     // class name is a must
  //        (classNamePrimaryExpression
  //         (  rangeReferenceFormWithClassPrefix
  //          | nameReference
  //          | relativeReference
  //          | indexReferenceClassForm
  //          | idReference
  //         )? //can not remove '?' from here.. as script compiles with just className as a primary expression...
  //        )
  private static boolean referenceForm_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "referenceForm_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = referenceForm_1_0_0(builder_, level_ + 1);
    result_ = result_ && referenceForm_1_0_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // my?
  private static boolean referenceForm_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "referenceForm_1_0_0")) return false;
    consumeToken(builder_, MY);
    return true;
  }

  // classNamePrimaryExpression
  //         (  rangeReferenceFormWithClassPrefix
  //          | nameReference
  //          | relativeReference
  //          | indexReferenceClassForm
  //          | idReference
  //         )?
  private static boolean referenceForm_1_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "referenceForm_1_0_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = classNamePrimaryExpression(builder_, level_ + 1);
    result_ = result_ && referenceForm_1_0_1_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (  rangeReferenceFormWithClassPrefix
  //          | nameReference
  //          | relativeReference
  //          | indexReferenceClassForm
  //          | idReference
  //         )?
  private static boolean referenceForm_1_0_1_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "referenceForm_1_0_1_1")) return false;
    referenceForm_1_0_1_1_0(builder_, level_ + 1);
    return true;
  }

  // rangeReferenceFormWithClassPrefix
  //          | nameReference
  //          | relativeReference
  //          | indexReferenceClassForm
  //          | idReference
  private static boolean referenceForm_1_0_1_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "referenceForm_1_0_1_1_0")) return false;
    boolean result_;
    result_ = rangeReferenceFormWithClassPrefix(builder_, level_ + 1);
    if (!result_) result_ = nameReference(builder_, level_ + 1);
    if (!result_) result_ = relativeReference(builder_, level_ + 1);
    if (!result_) result_ = indexReferenceClassForm(builder_, level_ + 1);
    if (!result_) result_ = idReference(builder_, level_ + 1);
    return result_;
  }

  // arbitraryReference
  //         | everyElemReference
  //         | everyRangeReference
  //         | middleElemReference
  //         | relativeReference
  //         | indexReference
  //         | propertyReference
  private static boolean referenceForm_1_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "referenceForm_1_1")) return false;
    boolean result_;
    result_ = arbitraryReference(builder_, level_ + 1);
    if (!result_) result_ = everyElemReference(builder_, level_ + 1);
    if (!result_) result_ = everyRangeReference(builder_, level_ + 1);
    if (!result_) result_ = middleElemReference(builder_, level_ + 1);
    if (!result_) result_ = relativeReference(builder_, level_ + 1);
    if (!result_) result_ = indexReference(builder_, level_ + 1);
    if (!result_) result_ = propertyReference(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // referenceExpression &handlerParameterLabel
  static boolean referenceIdBeforeParamLabel(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "referenceIdBeforeParamLabel")) return false;
    if (!nextTokenIs(builder_, VAR_IDENTIFIER)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = referenceExpression(builder_, level_ + 1);
    result_ = result_ && referenceIdBeforeParamLabel_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // &handlerParameterLabel
  private static boolean referenceIdBeforeParamLabel_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "referenceIdBeforeParamLabel_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _AND_);
    result_ = handlerParameterLabel(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // applicationReference
  static boolean referenceToApplicationVar(PsiBuilder builder_, int level_) {
    return applicationReference(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // LT|GT|LE|GE|containment_start_end_operator|containment_any_part_operator
  static boolean relational_operator(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "relational_operator")) return false;
    boolean result_;
    result_ = consumeToken(builder_, LT);
    if (!result_) result_ = consumeToken(builder_, GT);
    if (!result_) result_ = consumeToken(builder_, LE);
    if (!result_) result_ = consumeToken(builder_, GE);
    if (!result_) result_ = containment_start_end_operator(builder_, level_ + 1);
    if (!result_) result_ = containment_any_part_operator(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // (after|([in]( back | end ) of)|behind )primaryReferenceExpression
  static boolean relativeAfterReference(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "relativeAfterReference")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = relativeAfterReference_0(builder_, level_ + 1);
    result_ = result_ && primaryReferenceExpression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // after|([in]( back | end ) of)|behind
  private static boolean relativeAfterReference_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "relativeAfterReference_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, AFTER);
    if (!result_) result_ = relativeAfterReference_0_1(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, BEHIND);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // [in]( back | end ) of
  private static boolean relativeAfterReference_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "relativeAfterReference_0_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = relativeAfterReference_0_1_0(builder_, level_ + 1);
    result_ = result_ && relativeAfterReference_0_1_1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, OF);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // [in]
  private static boolean relativeAfterReference_0_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "relativeAfterReference_0_1_0")) return false;
    consumeToken(builder_, IN);
    return true;
  }

  // back | end
  private static boolean relativeAfterReference_0_1_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "relativeAfterReference_0_1_1")) return false;
    boolean result_;
    result_ = consumeToken(builder_, BACK);
    if (!result_) result_ = consumeToken(builder_, END);
    return result_;
  }

  /* ********************************************************** */
  // (before|([in] ( front | beginning ) of) )primaryReferenceExpression
  static boolean relativeBeforeReference(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "relativeBeforeReference")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = relativeBeforeReference_0(builder_, level_ + 1);
    result_ = result_ && primaryReferenceExpression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // before|([in] ( front | beginning ) of)
  private static boolean relativeBeforeReference_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "relativeBeforeReference_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, BEFORE);
    if (!result_) result_ = relativeBeforeReference_0_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // [in] ( front | beginning ) of
  private static boolean relativeBeforeReference_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "relativeBeforeReference_0_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = relativeBeforeReference_0_1_0(builder_, level_ + 1);
    result_ = result_ && relativeBeforeReference_0_1_1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, OF);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // [in]
  private static boolean relativeBeforeReference_0_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "relativeBeforeReference_0_1_0")) return false;
    consumeToken(builder_, IN);
    return true;
  }

  // front | beginning
  private static boolean relativeBeforeReference_0_1_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "relativeBeforeReference_0_1_1")) return false;
    boolean result_;
    result_ = consumeToken(builder_, FRONT);
    if (!result_) result_ = consumeToken(builder_, BEGINNING);
    return result_;
  }

  /* ********************************************************** */
  // relativeBeforeReference | relativeAfterReference
  public static boolean relativeReference(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "relativeReference")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _COLLAPSE_, RELATIVE_REFERENCE, "<relative reference>");
    result_ = relativeBeforeReference(builder_, level_ + 1);
    if (!result_) result_ = relativeAfterReference(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // REPEAT sep
  //                             blockBody?
  //                            end [REPEAT]
  public static boolean repeatForeverStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "repeatForeverStatement")) return false;
    if (!nextTokenIs(builder_, REPEAT)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, REPEAT);
    result_ = result_ && sep(builder_, level_ + 1);
    result_ = result_ && repeatForeverStatement_2(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, END);
    result_ = result_ && repeatForeverStatement_4(builder_, level_ + 1);
    exit_section_(builder_, marker_, REPEAT_FOREVER_STATEMENT, result_);
    return result_;
  }

  // blockBody?
  private static boolean repeatForeverStatement_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "repeatForeverStatement_2")) return false;
    blockBody(builder_, level_ + 1);
    return true;
  }

  // [REPEAT]
  private static boolean repeatForeverStatement_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "repeatForeverStatement_4")) return false;
    consumeToken(builder_, REPEAT);
    return true;
  }

  /* ********************************************************** */
  // REPEAT numTimes [times] sep
  //                             blockBody?
  //                             end [REPEAT]
  public static boolean repeatNumTimesStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "repeatNumTimesStatement")) return false;
    if (!nextTokenIs(builder_, REPEAT)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, REPEAT);
    result_ = result_ && numTimes(builder_, level_ + 1);
    result_ = result_ && repeatNumTimesStatement_2(builder_, level_ + 1);
    result_ = result_ && sep(builder_, level_ + 1);
    result_ = result_ && repeatNumTimesStatement_4(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, END);
    result_ = result_ && repeatNumTimesStatement_6(builder_, level_ + 1);
    exit_section_(builder_, marker_, REPEAT_NUM_TIMES_STATEMENT, result_);
    return result_;
  }

  // [times]
  private static boolean repeatNumTimesStatement_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "repeatNumTimesStatement_2")) return false;
    consumeToken(builder_, TIMES);
    return true;
  }

  // blockBody?
  private static boolean repeatNumTimesStatement_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "repeatNumTimesStatement_4")) return false;
    blockBody(builder_, level_ + 1);
    return true;
  }

  // [REPEAT]
  private static boolean repeatNumTimesStatement_6(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "repeatNumTimesStatement_6")) return false;
    consumeToken(builder_, REPEAT);
    return true;
  }

  /* ********************************************************** */
  // repeatNumTimesStatement | repeatWhileStatement | repeatUntilStatement
  // | repeatWithRangeStatement | repeatWithListStatement | repeatForeverStatement
  static boolean repeatStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "repeatStatement")) return false;
    if (!nextTokenIs(builder_, REPEAT)) return false;
    boolean result_;
    result_ = repeatNumTimesStatement(builder_, level_ + 1);
    if (!result_) result_ = repeatWhileStatement(builder_, level_ + 1);
    if (!result_) result_ = repeatUntilStatement(builder_, level_ + 1);
    if (!result_) result_ = repeatWithRangeStatement(builder_, level_ + 1);
    if (!result_) result_ = repeatWithListStatement(builder_, level_ + 1);
    if (!result_) result_ = repeatForeverStatement(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // REPEAT until expression sep
  //                          blockBody?
  //                          end [REPEAT]
  public static boolean repeatUntilStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "repeatUntilStatement")) return false;
    if (!nextTokenIs(builder_, REPEAT)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, REPEAT_UNTIL_STATEMENT, null);
    result_ = consumeTokens(builder_, 2, REPEAT, UNTIL);
    pinned_ = result_; // pin = 2
    result_ = result_ && report_error_(builder_, expression(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, sep(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, repeatUntilStatement_4(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, END)) && result_;
    result_ = pinned_ && repeatUntilStatement_6(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // blockBody?
  private static boolean repeatUntilStatement_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "repeatUntilStatement_4")) return false;
    blockBody(builder_, level_ + 1);
    return true;
  }

  // [REPEAT]
  private static boolean repeatUntilStatement_6(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "repeatUntilStatement_6")) return false;
    consumeToken(builder_, REPEAT);
    return true;
  }

  /* ********************************************************** */
  // REPEAT while expression sep
  //                             blockBody?
  //                          end [REPEAT]
  public static boolean repeatWhileStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "repeatWhileStatement")) return false;
    if (!nextTokenIs(builder_, REPEAT)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, REPEAT_WHILE_STATEMENT, null);
    result_ = consumeTokens(builder_, 2, REPEAT, WHILE);
    pinned_ = result_; // pin = 2
    result_ = result_ && report_error_(builder_, expression(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, sep(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, repeatWhileStatement_4(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, END)) && result_;
    result_ = pinned_ && repeatWhileStatement_6(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // blockBody?
  private static boolean repeatWhileStatement_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "repeatWhileStatement_4")) return false;
    blockBody(builder_, level_ + 1);
    return true;
  }

  // [REPEAT]
  private static boolean repeatWhileStatement_6(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "repeatWhileStatement_6")) return false;
    consumeToken(builder_, REPEAT);
    return true;
  }

  /* ********************************************************** */
  // REPEAT with loopVariable in listOrReferenceExpression sep
  //                          blockBody?
  //                          end [REPEAT]
  public static boolean repeatWithListStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "repeatWithListStatement")) return false;
    if (!nextTokenIs(builder_, REPEAT)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, REPEAT_WITH_LIST_STATEMENT, null);
    result_ = consumeTokens(builder_, 0, REPEAT, WITH);
    result_ = result_ && loopVariable(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, IN);
    pinned_ = result_; // pin = 4
    result_ = result_ && report_error_(builder_, listOrReferenceExpression(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, sep(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, repeatWithListStatement_6(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, END)) && result_;
    result_ = pinned_ && repeatWithListStatement_8(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // blockBody?
  private static boolean repeatWithListStatement_6(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "repeatWithListStatement_6")) return false;
    blockBody(builder_, level_ + 1);
    return true;
  }

  // [REPEAT]
  private static boolean repeatWithListStatement_8(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "repeatWithListStatement_8")) return false;
    consumeToken(builder_, REPEAT);
    return true;
  }

  /* ********************************************************** */
  // REPEAT with loopVariable from startValue to stopValue [by stepValue] sep
  //                              blockBody?
  //                              end [REPEAT]
  public static boolean repeatWithRangeStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "repeatWithRangeStatement")) return false;
    if (!nextTokenIs(builder_, REPEAT)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, REPEAT_WITH_RANGE_STATEMENT, null);
    result_ = consumeTokens(builder_, 0, REPEAT, WITH);
    result_ = result_ && loopVariable(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, FROM);
    pinned_ = result_; // pin = 4
    result_ = result_ && report_error_(builder_, startValue(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, TO)) && result_;
    result_ = pinned_ && report_error_(builder_, stopValue(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, repeatWithRangeStatement_7(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, sep(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, repeatWithRangeStatement_9(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, END)) && result_;
    result_ = pinned_ && repeatWithRangeStatement_11(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // [by stepValue]
  private static boolean repeatWithRangeStatement_7(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "repeatWithRangeStatement_7")) return false;
    repeatWithRangeStatement_7_0(builder_, level_ + 1);
    return true;
  }

  // by stepValue
  private static boolean repeatWithRangeStatement_7_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "repeatWithRangeStatement_7_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, BY);
    result_ = result_ && stepValue(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // blockBody?
  private static boolean repeatWithRangeStatement_9(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "repeatWithRangeStatement_9")) return false;
    blockBody(builder_, level_ + 1);
    return true;
  }

  // [REPEAT]
  private static boolean repeatWithRangeStatement_11(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "repeatWithRangeStatement_11")) return false;
    consumeToken(builder_, REPEAT);
    return true;
  }

  /* ********************************************************** */
  // expression
  static boolean resultList(PsiBuilder builder_, int level_) {
    return expression(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // directParameterDeclaration|expression
  static boolean resultListVar(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "resultListVar")) return false;
    boolean result_;
    result_ = directParameterDeclaration(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // result
  static boolean resultProperty(PsiBuilder builder_, int level_) {
    return consumeToken(builder_, RESULT);
  }

  /* ********************************************************** */
  // return [expression]
  public static boolean returnStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "returnStatement")) return false;
    if (!nextTokenIs(builder_, RETURN)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, RETURN);
    result_ = result_ && returnStatement_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, RETURN_STATEMENT, result_);
    return result_;
  }

  // [expression]
  private static boolean returnStatement_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "returnStatement_1")) return false;
    expression(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // ("run" [script] [script_object_variable])|("run" [referenceToApplicationVar])
  public static boolean runCommandExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "runCommandExpression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, RUN_COMMAND_EXPRESSION, "<run command expression>");
    result_ = runCommandExpression_0(builder_, level_ + 1);
    if (!result_) result_ = runCommandExpression_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // "run" [script] [script_object_variable]
  private static boolean runCommandExpression_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "runCommandExpression_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, "run");
    result_ = result_ && runCommandExpression_0_1(builder_, level_ + 1);
    result_ = result_ && runCommandExpression_0_2(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // [script]
  private static boolean runCommandExpression_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "runCommandExpression_0_1")) return false;
    consumeTokenFast(builder_, SCRIPT);
    return true;
  }

  // [script_object_variable]
  private static boolean runCommandExpression_0_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "runCommandExpression_0_2")) return false;
    script_object_variable(builder_, level_ + 1);
    return true;
  }

  // "run" [referenceToApplicationVar]
  private static boolean runCommandExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "runCommandExpression_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, "run");
    result_ = result_ && runCommandExpression_1_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // [referenceToApplicationVar]
  private static boolean runCommandExpression_1_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "runCommandExpression_1_1")) return false;
    referenceToApplicationVar(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // topBlockBodyPart (sep topBlockBodyPart)* sep?
  public static boolean scriptBody(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "scriptBody")) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, SCRIPT_BODY, "<script body>");
    result_ = topBlockBodyPart(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, scriptBody_1(builder_, level_ + 1));
    result_ = pinned_ && scriptBody_2(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // (sep topBlockBodyPart)*
  private static boolean scriptBody_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "scriptBody_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!scriptBody_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "scriptBody_1", pos_)) break;
    }
    return true;
  }

  // sep topBlockBodyPart
  private static boolean scriptBody_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "scriptBody_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = sep(builder_, level_ + 1);
    result_ = result_ && topBlockBodyPart(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // sep?
  private static boolean scriptBody_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "scriptBody_2")) return false;
    sep(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // script scriptObjectName sep
  //                                   scriptBody?
  //                            end [script]
  public static boolean scriptObjectDefinition(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "scriptObjectDefinition")) return false;
    if (!nextTokenIs(builder_, SCRIPT)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, SCRIPT);
    result_ = result_ && scriptObjectName(builder_, level_ + 1);
    result_ = result_ && sep(builder_, level_ + 1);
    result_ = result_ && scriptObjectDefinition_3(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, END);
    result_ = result_ && scriptObjectDefinition_5(builder_, level_ + 1);
    exit_section_(builder_, marker_, SCRIPT_OBJECT_DEFINITION, result_);
    return result_;
  }

  // scriptBody?
  private static boolean scriptObjectDefinition_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "scriptObjectDefinition_3")) return false;
    scriptBody(builder_, level_ + 1);
    return true;
  }

  // [script]
  private static boolean scriptObjectDefinition_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "scriptObjectDefinition_5")) return false;
    consumeToken(builder_, SCRIPT);
    return true;
  }

  /* ********************************************************** */
  // identifier
  static boolean scriptObjectName(PsiBuilder builder_, int level_) {
    return identifier(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // script sep
  //                                   scriptBody?
  //                                  end [script]
  public static boolean scriptObjectUnnamedDefinition(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "scriptObjectUnnamedDefinition")) return false;
    if (!nextTokenIs(builder_, SCRIPT)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, SCRIPT_OBJECT_UNNAMED_DEFINITION, null);
    result_ = consumeToken(builder_, SCRIPT);
    result_ = result_ && sep(builder_, level_ + 1);
    result_ = result_ && scriptObjectUnnamedDefinition_2(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, END);
    pinned_ = result_; // pin = 4
    result_ = result_ && scriptObjectUnnamedDefinition_4(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // scriptBody?
  private static boolean scriptObjectUnnamedDefinition_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "scriptObjectUnnamedDefinition_2")) return false;
    scriptBody(builder_, level_ + 1);
    return true;
  }

  // [script]
  private static boolean scriptObjectUnnamedDefinition_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "scriptObjectUnnamedDefinition_4")) return false;
    consumeToken(builder_, SCRIPT);
    return true;
  }

  /* ********************************************************** */
  // (property|prop) propertyLabelIdentifier [COLON propertyInitializerExpression]
  public static boolean scriptPropertyDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "scriptPropertyDeclaration")) return false;
    if (!nextTokenIs(builder_, "<script property declaration>", PROP, PROPERTY)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, SCRIPT_PROPERTY_DECLARATION, "<script property declaration>");
    result_ = scriptPropertyDeclaration_0(builder_, level_ + 1);
    result_ = result_ && propertyLabelIdentifier(builder_, level_ + 1);
    result_ = result_ && scriptPropertyDeclaration_2(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // property|prop
  private static boolean scriptPropertyDeclaration_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "scriptPropertyDeclaration_0")) return false;
    boolean result_;
    result_ = consumeToken(builder_, PROPERTY);
    if (!result_) result_ = consumeToken(builder_, PROP);
    return result_;
  }

  // [COLON propertyInitializerExpression]
  private static boolean scriptPropertyDeclaration_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "scriptPropertyDeclaration_2")) return false;
    scriptPropertyDeclaration_2_0(builder_, level_ + 1);
    return true;
  }

  // COLON propertyInitializerExpression
  private static boolean scriptPropertyDeclaration_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "scriptPropertyDeclaration_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, COLON);
    result_ = result_ && propertyInitializerExpression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // referenceExpression
  static boolean script_object_variable(PsiBuilder builder_, int level_) {
    return referenceExpression(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // <<parseExpression '"scripting additions"' SCRIPTING_ADDITIONS>>
  static boolean scriptingAdditionsFolderConstant(PsiBuilder builder_, int level_) {
    return parseExpression(builder_, level_ + 1, "scripting additions", SCRIPTING_ADDITIONS_parser_);
  }

  /* ********************************************************** */
  // MINUTES_CONSTANT|HOURS_CONSTANT|DAYS_CONSTANT|WEEKS_CONSTANT
  static boolean seconds_constants(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "seconds_constants")) return false;
    boolean result_;
    result_ = consumeToken(builder_, MINUTES_CONSTANT);
    if (!result_) result_ = consumeToken(builder_, HOURS_CONSTANT);
    if (!result_) result_ = consumeToken(builder_, DAYS_CONSTANT);
    if (!result_) result_ = consumeToken(builder_, WEEKS_CONSTANT);
    return result_;
  }

  /* ********************************************************** */
  // identifier
  public static boolean selectorId(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "selectorId")) return false;
    if (!nextTokenIs(builder_, VAR_IDENTIFIER)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = identifier(builder_, level_ + 1);
    exit_section_(builder_, marker_, SELECTOR_ID, result_);
    return result_;
  }

  /* ********************************************************** */
  // (COMMENT? NLS COMMENT?)+
  static boolean sep(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "sep")) return false;
    if (!nextTokenIs(builder_, "", COMMENT, NLS)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = sep_0(builder_, level_ + 1);
    while (result_) {
      int pos_ = current_position_(builder_);
      if (!sep_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "sep", pos_)) break;
    }
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // COMMENT? NLS COMMENT?
  private static boolean sep_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "sep_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = sep_0_0(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, NLS);
    result_ = result_ && sep_0_2(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // COMMENT?
  private static boolean sep_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "sep_0_0")) return false;
    consumeToken(builder_, COMMENT);
    return true;
  }

  // COMMENT?
  private static boolean sep_0_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "sep_0_2")) return false;
    consumeToken(builder_, COMMENT);
    return true;
  }

  /* ********************************************************** */
  // expression
  static boolean sessionSpecificator(PsiBuilder builder_, int level_) {
    return expression(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // setCommandAppleScriptSetSyntax|setCommandAppleScriptReturningSyntax
  static boolean setCommandAppleScript(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "setCommandAppleScript")) return false;
    boolean result_;
    result_ = setCommandAppleScriptSetSyntax(builder_, level_ + 1);
    if (!result_) result_ = setCommandAppleScriptReturningSyntax(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // expression returning targetVariablePattern
  static boolean setCommandAppleScriptReturningSyntax(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "setCommandAppleScriptReturningSyntax")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = expression(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, RETURNING);
    result_ = result_ && targetVariablePattern(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // set THE_KW? (targetVariablePattern to|objectReferenceWrapper to) THE_KW? expression
  static boolean setCommandAppleScriptSetSyntax(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "setCommandAppleScriptSetSyntax")) return false;
    if (!nextTokenIs(builder_, SET)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, SET);
    result_ = result_ && setCommandAppleScriptSetSyntax_1(builder_, level_ + 1);
    result_ = result_ && setCommandAppleScriptSetSyntax_2(builder_, level_ + 1);
    result_ = result_ && setCommandAppleScriptSetSyntax_3(builder_, level_ + 1);
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // THE_KW?
  private static boolean setCommandAppleScriptSetSyntax_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "setCommandAppleScriptSetSyntax_1")) return false;
    consumeToken(builder_, THE_KW);
    return true;
  }

  // targetVariablePattern to|objectReferenceWrapper to
  private static boolean setCommandAppleScriptSetSyntax_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "setCommandAppleScriptSetSyntax_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = setCommandAppleScriptSetSyntax_2_0(builder_, level_ + 1);
    if (!result_) result_ = setCommandAppleScriptSetSyntax_2_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // targetVariablePattern to
  private static boolean setCommandAppleScriptSetSyntax_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "setCommandAppleScriptSetSyntax_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = targetVariablePattern(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, TO);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // objectReferenceWrapper to
  private static boolean setCommandAppleScriptSetSyntax_2_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "setCommandAppleScriptSetSyntax_2_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = objectReferenceWrapper(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, TO);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // THE_KW?
  private static boolean setCommandAppleScriptSetSyntax_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "setCommandAppleScriptSetSyntax_3")) return false;
    consumeToken(builder_, THE_KW);
    return true;
  }

  /* ********************************************************** */
  // THE_KW? identifier
  public static boolean simpleFormalParameter(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "simpleFormalParameter")) return false;
    if (!nextTokenIs(builder_, "<simple formal parameter>", THE_KW, VAR_IDENTIFIER)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, SIMPLE_FORMAL_PARAMETER, "<simple formal parameter>");
    result_ = simpleFormalParameter_0(builder_, level_ + 1);
    result_ = result_ && identifier(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // THE_KW?
  private static boolean simpleFormalParameter_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "simpleFormalParameter_0")) return false;
    consumeToken(builder_, THE_KW);
    return true;
  }

  /* ********************************************************** */
  // (dictionaryClassName | builtInClassIdentifier | rawClassExpression) ITEM?
  static boolean singularClassName(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "singularClassName")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = singularClassName_0(builder_, level_ + 1);
    result_ = result_ && singularClassName_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // dictionaryClassName | builtInClassIdentifier | rawClassExpression
  private static boolean singularClassName_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "singularClassName_0")) return false;
    boolean result_;
    result_ = dictionaryClassName(builder_, level_ + 1);
    if (!result_) result_ = builtInClassIdentifier(builder_, level_ + 1);
    if (!result_) result_ = rawClassExpression(builder_, level_ + 1);
    return result_;
  }

  // ITEM?
  private static boolean singularClassName_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "singularClassName_1")) return false;
    consumeToken(builder_, ITEM);
    return true;
  }

  /* ********************************************************** */
  // integerLiteralExpression | expression
  static boolean startIndex(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "startIndex")) return false;
    boolean result_;
    result_ = integerLiteralExpression(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // (referenceExpression &to) | expression
  static boolean startValue(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "startValue")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = startValue_0(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // referenceExpression &to
  private static boolean startValue_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "startValue_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = referenceExpression(builder_, level_ + 1);
    result_ = result_ && startValue_0_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // &to
  private static boolean startValue_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "startValue_0_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _AND_);
    result_ = consumeToken(builder_, TO);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // controlStatement|scriptPropertyDeclaration|varDeclarationList
  //                       |<<parseExpression '"script"' (scriptObjectDefinition | scriptObjectUnnamedDefinition)>>
  //                       |returnStatement|continue_statement
  //                       |parseAssignmentStatement
  static boolean statement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "statement")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = controlStatement(builder_, level_ + 1);
    if (!result_) result_ = scriptPropertyDeclaration(builder_, level_ + 1);
    if (!result_) result_ = varDeclarationList(builder_, level_ + 1);
    if (!result_) result_ = parseExpression(builder_, level_ + 1, "script", AppleScriptParser::statement_3_1);
    if (!result_) result_ = returnStatement(builder_, level_ + 1);
    if (!result_) result_ = continue_statement(builder_, level_ + 1);
    if (!result_) result_ = parseAssignmentStatement(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // scriptObjectDefinition | scriptObjectUnnamedDefinition
  private static boolean statement_3_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "statement_3_1")) return false;
    boolean result_;
    result_ = scriptObjectDefinition(builder_, level_ + 1);
    if (!result_) result_ = scriptObjectUnnamedDefinition(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // expression
  static boolean stepValue(PsiBuilder builder_, int level_) {
    return expression(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // integerLiteralExpression | expression
  static boolean stopIndex(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "stopIndex")) return false;
    boolean result_;
    result_ = integerLiteralExpression(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // (referenceExpression &by) | expression
  static boolean stopValue(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "stopValue")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = stopValue_0(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // referenceExpression &by
  private static boolean stopValue_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "stopValue_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = referenceExpression(builder_, level_ + 1);
    result_ = result_ && stopValue_0_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // &by
  private static boolean stopValue_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "stopValue_0_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _AND_);
    result_ = consumeToken(builder_, BY);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // STRING_LITERAL
  public static boolean stringLiteralExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "stringLiteralExpression")) return false;
    if (!nextTokenIsFast(builder_, STRING_LITERAL)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokenFast(builder_, STRING_LITERAL);
    exit_section_(builder_, marker_, STRING_LITERAL_EXPRESSION, result_);
    return result_;
  }

  /* ********************************************************** */
  // QUOTED_FORM
  static boolean stringProperty(PsiBuilder builder_, int level_) {
    return consumeToken(builder_, QUOTED_FORM);
  }

  /* ********************************************************** */
  // STRING_LITERAL
  static boolean styledTextLiteralExpression(PsiBuilder builder_, int level_) {
    return consumeTokenFast(builder_, STRING_LITERAL);
  }

  /* ********************************************************** */
  // LCURLY [targetVariablePattern|expression] (COMMA targetVariablePattern|expression)* RCURLY
  public static boolean targetListLiteral(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "targetListLiteral")) return false;
    if (!nextTokenIs(builder_, LCURLY)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, LCURLY);
    result_ = result_ && targetListLiteral_1(builder_, level_ + 1);
    result_ = result_ && targetListLiteral_2(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, RCURLY);
    exit_section_(builder_, marker_, TARGET_LIST_LITERAL, result_);
    return result_;
  }

  // [targetVariablePattern|expression]
  private static boolean targetListLiteral_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "targetListLiteral_1")) return false;
    targetListLiteral_1_0(builder_, level_ + 1);
    return true;
  }

  // targetVariablePattern|expression
  private static boolean targetListLiteral_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "targetListLiteral_1_0")) return false;
    boolean result_;
    result_ = targetVariablePattern(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    return result_;
  }

  // (COMMA targetVariablePattern|expression)*
  private static boolean targetListLiteral_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "targetListLiteral_2")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!targetListLiteral_2_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "targetListLiteral_2", pos_)) break;
    }
    return true;
  }

  // COMMA targetVariablePattern|expression
  private static boolean targetListLiteral_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "targetListLiteral_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = targetListLiteral_2_0_0(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // COMMA targetVariablePattern
  private static boolean targetListLiteral_2_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "targetListLiteral_2_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, COMMA);
    result_ = result_ && targetVariablePattern(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // LCURLY[objectTargetPropertyDeclaration] (COMMA objectTargetPropertyDeclaration)* RCURLY
  public static boolean targetRecordLiteral(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "targetRecordLiteral")) return false;
    if (!nextTokenIs(builder_, LCURLY)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, LCURLY);
    result_ = result_ && targetRecordLiteral_1(builder_, level_ + 1);
    result_ = result_ && targetRecordLiteral_2(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, RCURLY);
    exit_section_(builder_, marker_, TARGET_RECORD_LITERAL, result_);
    return result_;
  }

  // [objectTargetPropertyDeclaration]
  private static boolean targetRecordLiteral_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "targetRecordLiteral_1")) return false;
    objectTargetPropertyDeclaration(builder_, level_ + 1);
    return true;
  }

  // (COMMA objectTargetPropertyDeclaration)*
  private static boolean targetRecordLiteral_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "targetRecordLiteral_2")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!targetRecordLiteral_2_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "targetRecordLiteral_2", pos_)) break;
    }
    return true;
  }

  // COMMA objectTargetPropertyDeclaration
  private static boolean targetRecordLiteral_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "targetRecordLiteral_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, COMMA);
    result_ = result_ && objectTargetPropertyDeclaration(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // identifier
  public static boolean targetVariable(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "targetVariable")) return false;
    if (!nextTokenIs(builder_, VAR_IDENTIFIER)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = identifier(builder_, level_ + 1);
    exit_section_(builder_, marker_, TARGET_VARIABLE, result_);
    return result_;
  }

  /* ********************************************************** */
  // targetVariable|targetListLiteral|targetRecordLiteral
  static boolean targetVariablePattern(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "targetVariablePattern")) return false;
    if (!nextTokenIs(builder_, "", LCURLY, VAR_IDENTIFIER)) return false;
    boolean result_;
    result_ = targetVariable(builder_, level_ + 1);
    if (!result_) result_ = targetListLiteral(builder_, level_ + 1);
    if (!result_) result_ = targetRecordLiteral(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // tell objectReferenceWrapper sep
  //                                         blockBody?
  //                           end [tell]
  public static boolean tellCompoundStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tellCompoundStatement")) return false;
    if (!nextTokenIs(builder_, TELL)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, TELL);
    result_ = result_ && objectReferenceWrapper(builder_, level_ + 1);
    result_ = result_ && sep(builder_, level_ + 1);
    result_ = result_ && tellCompoundStatement_3(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, END);
    result_ = result_ && tellCompoundStatement_5(builder_, level_ + 1);
    exit_section_(builder_, marker_, TELL_COMPOUND_STATEMENT, result_);
    return result_;
  }

  // blockBody?
  private static boolean tellCompoundStatement_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tellCompoundStatement_3")) return false;
    blockBody(builder_, level_ + 1);
    return true;
  }

  // [tell]
  private static boolean tellCompoundStatement_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tellCompoundStatement_5")) return false;
    consumeToken(builder_, TELL);
    return true;
  }

  /* ********************************************************** */
  // tell <<parseTellSimpleObjectReference>> to (statement | handlerInterleavedParametersCall | expression )
  public static boolean tellSimpleStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tellSimpleStatement")) return false;
    if (!nextTokenIs(builder_, TELL)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, TELL);
    result_ = result_ && parseTellSimpleObjectReference(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, TO);
    result_ = result_ && tellSimpleStatement_3(builder_, level_ + 1);
    exit_section_(builder_, marker_, TELL_SIMPLE_STATEMENT, result_);
    return result_;
  }

  // statement | handlerInterleavedParametersCall | expression
  private static boolean tellSimpleStatement_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tellSimpleStatement_3")) return false;
    boolean result_;
    result_ = statement(builder_, level_ + 1);
    if (!result_) result_ = handlerInterleavedParametersCall(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // parseTellSimpleStatement|<<parseTellCompoundStatement>>
  static boolean tellStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tellStatement")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = parseTellSimpleStatement(builder_, level_ + 1);
    if (!result_) result_ = parseTellCompoundStatement(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // <<isTellStatementStart>>
  static boolean tellStatementStartCondition(PsiBuilder builder_, int level_) {
    return isTellStatementStart(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // TEXT_ITEM_DELIMETERS
  public static boolean textItemDelimitersProperty(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "textItemDelimitersProperty")) return false;
    if (!nextTokenIs(builder_, TEXT_ITEM_DELIMETERS)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, TEXT_ITEM_DELIMETERS);
    exit_section_(builder_, marker_, TEXT_ITEM_DELIMITERS_PROPERTY, result_);
    return result_;
  }

  /* ********************************************************** */
  // return|space|tab|linefeed|quote
  static boolean text_constant(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "text_constant")) return false;
    boolean result_;
    result_ = consumeToken(builder_, RETURN);
    if (!result_) result_ = consumeToken(builder_, SPACE);
    if (!result_) result_ = consumeToken(builder_, TAB);
    if (!result_) result_ = consumeToken(builder_, LINEFEED);
    if (!result_) result_ = consumeToken(builder_, QUOTE);
    return result_;
  }

  /* ********************************************************** */
  // expression
  static boolean timeoutIntegerExpression(PsiBuilder builder_, int level_) {
    return expression(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // topBlockBodyPart sep (topBlockBodyPart sep)*
  public static boolean topBlockBody(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "topBlockBody")) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, TOP_BLOCK_BODY, "<top block body>");
    result_ = topBlockBodyPart(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, sep(builder_, level_ + 1));
    result_ = pinned_ && topBlockBody_2(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // (topBlockBodyPart sep)*
  private static boolean topBlockBody_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "topBlockBody_2")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!topBlockBody_2_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "topBlockBody_2", pos_)) break;
    }
    return true;
  }

  // topBlockBodyPart sep
  private static boolean topBlockBody_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "topBlockBody_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = topBlockBodyPart(builder_, level_ + 1);
    result_ = result_ && sep(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // topLevelStatement | statement | expression | incompleteExpression
  static boolean topBlockBodyPart(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "topBlockBodyPart")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_);
    result_ = topLevelStatement(builder_, level_ + 1);
    if (!result_) result_ = statement(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    if (!result_) result_ = incompleteExpression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, AppleScriptParser::topBodyPartRecover);
    return result_;
  }

  /* ********************************************************** */
  // !sep
  static boolean topBodyPartRecover(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "topBodyPartRecover")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_);
    result_ = !sep(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // applicationHandlerDefinition | userHandlerDefinition | <<parseUseStatement useStatement>>
  static boolean topLevelStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "topLevelStatement")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = applicationHandlerDefinition(builder_, level_ + 1);
    if (!result_) result_ = userHandlerDefinition(builder_, level_ + 1);
    if (!result_) result_ = parseUseStatement(builder_, level_ + 1, AppleScriptParser::useStatement);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // try sep
  //                      blockBody?
  //                      (on error [errorMessageVar] [number errorNumberVar] [from offendingObjectVar]
  //                      [to expectedTypeVar] [partial result resultListVar] sep
  //                      [varDeclarationList sep]
  //                      blockBody?)?
  //                  end [error|try]
  public static boolean tryStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tryStatement")) return false;
    if (!nextTokenIs(builder_, TRY)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, TRY_STATEMENT, null);
    result_ = consumeToken(builder_, TRY);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, sep(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, tryStatement_2(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, tryStatement_3(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, END)) && result_;
    result_ = pinned_ && tryStatement_5(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // blockBody?
  private static boolean tryStatement_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tryStatement_2")) return false;
    blockBody(builder_, level_ + 1);
    return true;
  }

  // (on error [errorMessageVar] [number errorNumberVar] [from offendingObjectVar]
  //                      [to expectedTypeVar] [partial result resultListVar] sep
  //                      [varDeclarationList sep]
  //                      blockBody?)?
  private static boolean tryStatement_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tryStatement_3")) return false;
    tryStatement_3_0(builder_, level_ + 1);
    return true;
  }

  // on error [errorMessageVar] [number errorNumberVar] [from offendingObjectVar]
  //                      [to expectedTypeVar] [partial result resultListVar] sep
  //                      [varDeclarationList sep]
  //                      blockBody?
  private static boolean tryStatement_3_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tryStatement_3_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokens(builder_, 0, ON, ERROR);
    result_ = result_ && tryStatement_3_0_2(builder_, level_ + 1);
    result_ = result_ && tryStatement_3_0_3(builder_, level_ + 1);
    result_ = result_ && tryStatement_3_0_4(builder_, level_ + 1);
    result_ = result_ && tryStatement_3_0_5(builder_, level_ + 1);
    result_ = result_ && tryStatement_3_0_6(builder_, level_ + 1);
    result_ = result_ && sep(builder_, level_ + 1);
    result_ = result_ && tryStatement_3_0_8(builder_, level_ + 1);
    result_ = result_ && tryStatement_3_0_9(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // [errorMessageVar]
  private static boolean tryStatement_3_0_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tryStatement_3_0_2")) return false;
    errorMessageVar(builder_, level_ + 1);
    return true;
  }

  // [number errorNumberVar]
  private static boolean tryStatement_3_0_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tryStatement_3_0_3")) return false;
    tryStatement_3_0_3_0(builder_, level_ + 1);
    return true;
  }

  // number errorNumberVar
  private static boolean tryStatement_3_0_3_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tryStatement_3_0_3_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, NUMBER);
    result_ = result_ && errorNumberVar(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // [from offendingObjectVar]
  private static boolean tryStatement_3_0_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tryStatement_3_0_4")) return false;
    tryStatement_3_0_4_0(builder_, level_ + 1);
    return true;
  }

  // from offendingObjectVar
  private static boolean tryStatement_3_0_4_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tryStatement_3_0_4_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, FROM);
    result_ = result_ && offendingObjectVar(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // [to expectedTypeVar]
  private static boolean tryStatement_3_0_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tryStatement_3_0_5")) return false;
    tryStatement_3_0_5_0(builder_, level_ + 1);
    return true;
  }

  // to expectedTypeVar
  private static boolean tryStatement_3_0_5_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tryStatement_3_0_5_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, TO);
    result_ = result_ && expectedTypeVar(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // [partial result resultListVar]
  private static boolean tryStatement_3_0_6(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tryStatement_3_0_6")) return false;
    tryStatement_3_0_6_0(builder_, level_ + 1);
    return true;
  }

  // partial result resultListVar
  private static boolean tryStatement_3_0_6_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tryStatement_3_0_6_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokens(builder_, 0, PARTIAL, RESULT);
    result_ = result_ && resultListVar(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // [varDeclarationList sep]
  private static boolean tryStatement_3_0_8(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tryStatement_3_0_8")) return false;
    tryStatement_3_0_8_0(builder_, level_ + 1);
    return true;
  }

  // varDeclarationList sep
  private static boolean tryStatement_3_0_8_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tryStatement_3_0_8_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = varDeclarationList(builder_, level_ + 1);
    result_ = result_ && sep(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // blockBody?
  private static boolean tryStatement_3_0_9(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tryStatement_3_0_9")) return false;
    blockBody(builder_, level_ + 1);
    return true;
  }

  // [error|try]
  private static boolean tryStatement_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tryStatement_5")) return false;
    tryStatement_5_0(builder_, level_ + 1);
    return true;
  }

  // error|try
  private static boolean tryStatement_5_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tryStatement_5_0")) return false;
    boolean result_;
    result_ = consumeToken(builder_, ERROR);
    if (!result_) result_ = consumeToken(builder_, TRY);
    return result_;
  }

  /* ********************************************************** */
  // pluralClassName | singularClassName
  static boolean typeSpecifier(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeSpecifier")) return false;
    boolean result_;
    result_ = pluralClassName(builder_, level_ + 1);
    if (!result_) result_ = singularClassName(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // Length|SQUARE_AREA|CUBIC_VOL|LiquidVolume|Weight|TEMPERATURE
  static boolean unitTypeValueClasses(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "unitTypeValueClasses")) return false;
    boolean result_;
    result_ = Length(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, SQUARE_AREA);
    if (!result_) result_ = consumeToken(builder_, CUBIC_VOL);
    if (!result_) result_ = LiquidVolume(builder_, level_ + 1);
    if (!result_) result_ = Weight(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, TEMPERATURE);
    return result_;
  }

  /* ********************************************************** */
  // use 'AppleScript' [version expression]
  static boolean useAppleScriptStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "useAppleScriptStatement")) return false;
    if (!nextTokenIs(builder_, USE)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_);
    result_ = consumeToken(builder_, USE);
    result_ = result_ && consumeToken(builder_, "AppleScript");
    pinned_ = result_; // pin = 2
    result_ = result_ && useAppleScriptStatement_2(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // [version expression]
  private static boolean useAppleScriptStatement_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "useAppleScriptStatement_2")) return false;
    useAppleScriptStatement_2_0(builder_, level_ + 1);
    return true;
  }

  // version expression
  private static boolean useAppleScriptStatement_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "useAppleScriptStatement_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, VERSION);
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // use [directParameterDeclaration COLON]
  //                     (
  //                      (  <<parseUsedApplicationNameExternal withImportCondition>>//application "string" &(importing)
  //                       | dataSpecifier
  //                       | (script dataSpecifier)
  //                      )
  //                         [versionSpecifier] [(with importing)|(without importing)|(importing expression)]
  //                     )
  static boolean useApplicationOrScriptStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "useApplicationOrScriptStatement")) return false;
    if (!nextTokenIs(builder_, USE)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, USE);
    result_ = result_ && useApplicationOrScriptStatement_1(builder_, level_ + 1);
    result_ = result_ && useApplicationOrScriptStatement_2(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // [directParameterDeclaration COLON]
  private static boolean useApplicationOrScriptStatement_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "useApplicationOrScriptStatement_1")) return false;
    useApplicationOrScriptStatement_1_0(builder_, level_ + 1);
    return true;
  }

  // directParameterDeclaration COLON
  private static boolean useApplicationOrScriptStatement_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "useApplicationOrScriptStatement_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = directParameterDeclaration(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, COLON);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (  <<parseUsedApplicationNameExternal withImportCondition>>//application "string" &(importing)
  //                       | dataSpecifier
  //                       | (script dataSpecifier)
  //                      )
  //                         [versionSpecifier] [(with importing)|(without importing)|(importing expression)]
  private static boolean useApplicationOrScriptStatement_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "useApplicationOrScriptStatement_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = useApplicationOrScriptStatement_2_0(builder_, level_ + 1);
    result_ = result_ && useApplicationOrScriptStatement_2_1(builder_, level_ + 1);
    result_ = result_ && useApplicationOrScriptStatement_2_2(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // <<parseUsedApplicationNameExternal withImportCondition>>//application "string" &(importing)
  //                       | dataSpecifier
  //                       | (script dataSpecifier)
  private static boolean useApplicationOrScriptStatement_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "useApplicationOrScriptStatement_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = parseUsedApplicationNameExternal(builder_, level_ + 1, AppleScriptParser::withImportCondition);
    if (!result_) result_ = dataSpecifier(builder_, level_ + 1);
    if (!result_) result_ = useApplicationOrScriptStatement_2_0_2(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // script dataSpecifier
  private static boolean useApplicationOrScriptStatement_2_0_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "useApplicationOrScriptStatement_2_0_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, SCRIPT);
    result_ = result_ && dataSpecifier(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // [versionSpecifier]
  private static boolean useApplicationOrScriptStatement_2_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "useApplicationOrScriptStatement_2_1")) return false;
    versionSpecifier(builder_, level_ + 1);
    return true;
  }

  // [(with importing)|(without importing)|(importing expression)]
  private static boolean useApplicationOrScriptStatement_2_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "useApplicationOrScriptStatement_2_2")) return false;
    useApplicationOrScriptStatement_2_2_0(builder_, level_ + 1);
    return true;
  }

  // (with importing)|(without importing)|(importing expression)
  private static boolean useApplicationOrScriptStatement_2_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "useApplicationOrScriptStatement_2_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = useApplicationOrScriptStatement_2_2_0_0(builder_, level_ + 1);
    if (!result_) result_ = useApplicationOrScriptStatement_2_2_0_1(builder_, level_ + 1);
    if (!result_) result_ = useApplicationOrScriptStatement_2_2_0_2(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // with importing
  private static boolean useApplicationOrScriptStatement_2_2_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "useApplicationOrScriptStatement_2_2_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokens(builder_, 0, WITH, IMPORTING);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // without importing
  private static boolean useApplicationOrScriptStatement_2_2_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "useApplicationOrScriptStatement_2_2_0_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokens(builder_, 0, WITHOUT, IMPORTING);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // importing expression
  private static boolean useApplicationOrScriptStatement_2_2_0_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "useApplicationOrScriptStatement_2_2_0_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, IMPORTING);
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // use framework dataSpecifier
  static boolean useFrameworkStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "useFrameworkStatement")) return false;
    if (!nextTokenIs(builder_, USE)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_);
    result_ = consumeTokens(builder_, 2, USE, FRAMEWORK);
    pinned_ = result_; // pin = 2
    result_ = result_ && dataSpecifier(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // use SCRIPTING_ADDITIONS
  //                                    [((with importing)|(without importing)|importing) expression ]
  static boolean useScriptingAdditionsStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "useScriptingAdditionsStatement")) return false;
    if (!nextTokenIs(builder_, USE)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_);
    result_ = consumeTokens(builder_, 2, USE, SCRIPTING_ADDITIONS);
    pinned_ = result_; // pin = 2
    result_ = result_ && useScriptingAdditionsStatement_2(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // [((with importing)|(without importing)|importing) expression ]
  private static boolean useScriptingAdditionsStatement_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "useScriptingAdditionsStatement_2")) return false;
    useScriptingAdditionsStatement_2_0(builder_, level_ + 1);
    return true;
  }

  // ((with importing)|(without importing)|importing) expression
  private static boolean useScriptingAdditionsStatement_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "useScriptingAdditionsStatement_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = useScriptingAdditionsStatement_2_0_0(builder_, level_ + 1);
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (with importing)|(without importing)|importing
  private static boolean useScriptingAdditionsStatement_2_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "useScriptingAdditionsStatement_2_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = useScriptingAdditionsStatement_2_0_0_0(builder_, level_ + 1);
    if (!result_) result_ = useScriptingAdditionsStatement_2_0_0_1(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, IMPORTING);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // with importing
  private static boolean useScriptingAdditionsStatement_2_0_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "useScriptingAdditionsStatement_2_0_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokens(builder_, 0, WITH, IMPORTING);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // without importing
  private static boolean useScriptingAdditionsStatement_2_0_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "useScriptingAdditionsStatement_2_0_0_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokens(builder_, 0, WITHOUT, IMPORTING);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // useAppleScriptStatement|useApplicationOrScriptStatement|useFrameworkStatement
  public static boolean useStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "useStatement")) return false;
    if (!nextTokenIs(builder_, USE)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = useAppleScriptStatement(builder_, level_ + 1);
    if (!result_) result_ = useApplicationOrScriptStatement(builder_, level_ + 1);
    if (!result_) result_ = useFrameworkStatement(builder_, level_ + 1);
    exit_section_(builder_, marker_, USE_STATEMENT, result_);
    return result_;
  }

  /* ********************************************************** */
  // <<parseCheckForUseStatements>>
  static boolean useStatementsCondition(PsiBuilder builder_, int level_) {
    return parseCheckForUseStatements(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // identifier
  public static boolean userClassName(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "userClassName")) return false;
    if (!nextTokenIs(builder_, VAR_IDENTIFIER)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = identifier(builder_, level_ + 1);
    exit_section_(builder_, marker_, USER_CLASS_NAME, result_);
    return result_;
  }

  /* ********************************************************** */
  // handlerLabeledParametersDefinition | handlerPositionalParametersDefinition | handlerInterleavedParametersDefinition
  static boolean userHandlerDefinition(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "userHandlerDefinition")) return false;
    if (!nextTokenIs(builder_, "", ON, TO)) return false;
    boolean result_;
    result_ = handlerLabeledParametersDefinition(builder_, level_ + 1);
    if (!result_) result_ = handlerPositionalParametersDefinition(builder_, level_ + 1);
    if (!result_) result_ = handlerInterleavedParametersDefinition(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // THE_KW? referenceExpression
  static boolean userLabelReference(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "userLabelReference")) return false;
    if (!nextTokenIs(builder_, "", THE_KW, VAR_IDENTIFIER)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = userLabelReference_0(builder_, level_ + 1);
    result_ = result_ && referenceExpression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // THE_KW?
  private static boolean userLabelReference_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "userLabelReference_0")) return false;
    consumeToken(builder_, THE_KW);
    return true;
  }

  /* ********************************************************** */
  // expression
  static boolean userParameterVal(PsiBuilder builder_, int level_) {
    return expression(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // THE_KW? targetVariablePattern | expression
  static boolean userParameterVar(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "userParameterVar")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = userParameterVar_0(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // THE_KW? targetVariablePattern
  private static boolean userParameterVar_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "userParameterVar_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = userParameterVar_0_0(builder_, level_ + 1);
    result_ = result_ && targetVariablePattern(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // THE_KW?
  private static boolean userParameterVar_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "userParameterVar_0_0")) return false;
    consumeToken(builder_, THE_KW);
    return true;
  }

  /* ********************************************************** */
  // using terms from (applicationReference|(script dataSpecifier)|<<pushStdLibrary>>) sep
  //                              topBlockBody?
  //                             end [using terms from]
  public static boolean usingTermsFromStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "usingTermsFromStatement")) return false;
    if (!nextTokenIs(builder_, USING)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokens(builder_, 0, USING, TERMS, FROM);
    result_ = result_ && usingTermsFromStatement_3(builder_, level_ + 1);
    result_ = result_ && sep(builder_, level_ + 1);
    result_ = result_ && usingTermsFromStatement_5(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, END);
    result_ = result_ && usingTermsFromStatement_7(builder_, level_ + 1);
    exit_section_(builder_, marker_, USING_TERMS_FROM_STATEMENT, result_);
    return result_;
  }

  // applicationReference|(script dataSpecifier)|<<pushStdLibrary>>
  private static boolean usingTermsFromStatement_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "usingTermsFromStatement_3")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = applicationReference(builder_, level_ + 1);
    if (!result_) result_ = usingTermsFromStatement_3_1(builder_, level_ + 1);
    if (!result_) result_ = pushStdLibrary(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // script dataSpecifier
  private static boolean usingTermsFromStatement_3_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "usingTermsFromStatement_3_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, SCRIPT);
    result_ = result_ && dataSpecifier(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // topBlockBody?
  private static boolean usingTermsFromStatement_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "usingTermsFromStatement_5")) return false;
    topBlockBody(builder_, level_ + 1);
    return true;
  }

  // [using terms from]
  private static boolean usingTermsFromStatement_7(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "usingTermsFromStatement_7")) return false;
    parseTokens(builder_, 0, USING, TERMS, FROM);
    return true;
  }

  /* ********************************************************** */
  // primaryExpression filterReference? dictionaryCommandHandlerCallExpression?
  static boolean valueExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "valueExpression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = primaryExpression(builder_, level_ + 1);
    result_ = result_ && valueExpression_1(builder_, level_ + 1);
    result_ = result_ && valueExpression_2(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // filterReference?
  private static boolean valueExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "valueExpression_1")) return false;
    filterReference(builder_, level_ + 1);
    return true;
  }

  // dictionaryCommandHandlerCallExpression?
  private static boolean valueExpression_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "valueExpression_2")) return false;
    dictionaryCommandHandlerCallExpression(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // variableGlobalDeclaration|variableLocalDeclaration
  public static boolean varAccessDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "varAccessDeclaration")) return false;
    if (!nextTokenIs(builder_, "<var access declaration>", GLOBAL, LOCAL)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, VAR_ACCESS_DECLARATION, "<var access declaration>");
    result_ = variableGlobalDeclaration(builder_, level_ + 1);
    if (!result_) result_ = variableLocalDeclaration(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // varAccessDeclaration (COMMA varDeclarationListPart)*
  public static boolean varDeclarationList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "varDeclarationList")) return false;
    if (!nextTokenIs(builder_, "<var declaration list>", GLOBAL, LOCAL)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, VAR_DECLARATION_LIST, "<var declaration list>");
    result_ = varAccessDeclaration(builder_, level_ + 1);
    result_ = result_ && varDeclarationList_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // (COMMA varDeclarationListPart)*
  private static boolean varDeclarationList_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "varDeclarationList_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!varDeclarationList_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "varDeclarationList_1", pos_)) break;
    }
    return true;
  }

  // COMMA varDeclarationListPart
  private static boolean varDeclarationList_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "varDeclarationList_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, COMMA);
    result_ = result_ && varDeclarationListPart(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // identifier
  public static boolean varDeclarationListPart(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "varDeclarationListPart")) return false;
    if (!nextTokenIs(builder_, VAR_IDENTIFIER)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = identifier(builder_, level_ + 1);
    exit_section_(builder_, marker_, VAR_DECLARATION_LIST_PART, result_);
    return result_;
  }

  /* ********************************************************** */
  // global identifier
  static boolean variableGlobalDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "variableGlobalDeclaration")) return false;
    if (!nextTokenIs(builder_, GLOBAL)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, GLOBAL);
    result_ = result_ && identifier(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // local identifier
  static boolean variableLocalDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "variableLocalDeclaration")) return false;
    if (!nextTokenIs(builder_, LOCAL)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, LOCAL);
    result_ = result_ && identifier(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // version
  static boolean versionProperty(PsiBuilder builder_, int level_) {
    return consumeToken(builder_, VERSION);
  }

  /* ********************************************************** */
  // version stringLiteralExpression
  static boolean versionSpecifier(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "versionSpecifier")) return false;
    if (!nextTokenIs(builder_, VERSION)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, VERSION);
    result_ = result_ && stringLiteralExpression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // 'Monday'|'Tuesday'|'Wednesday'|'Thursday'|'Friday'|'Saturday'
  //                             |'Sunday'|'Mon'|'Tue'|'Wed'|'Thu'|'Fri'|'Sat'|'Sun'
  static boolean weekday_constant(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "weekday_constant")) return false;
    boolean result_;
    result_ = consumeToken(builder_, "Monday");
    if (!result_) result_ = consumeToken(builder_, "Tuesday");
    if (!result_) result_ = consumeToken(builder_, "Wednesday");
    if (!result_) result_ = consumeToken(builder_, "Thursday");
    if (!result_) result_ = consumeToken(builder_, "Friday");
    if (!result_) result_ = consumeToken(builder_, "Saturday");
    if (!result_) result_ = consumeToken(builder_, "Sunday");
    if (!result_) result_ = consumeToken(builder_, "Mon");
    if (!result_) result_ = consumeToken(builder_, "Tue");
    if (!result_) result_ = consumeToken(builder_, "Wed");
    if (!result_) result_ = consumeToken(builder_, "Thu");
    if (!result_) result_ = consumeToken(builder_, "Fri");
    if (!result_) result_ = consumeToken(builder_, "Sat");
    if (!result_) result_ = consumeToken(builder_, "Sun");
    return result_;
  }

  /* ********************************************************** */
  // &([versionSpecifier] !((without importing)|(importing false)))
  static boolean withImportCondition(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "withImportCondition")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _AND_);
    result_ = withImportCondition_0(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // [versionSpecifier] !((without importing)|(importing false))
  private static boolean withImportCondition_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "withImportCondition_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = withImportCondition_0_0(builder_, level_ + 1);
    result_ = result_ && withImportCondition_0_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // [versionSpecifier]
  private static boolean withImportCondition_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "withImportCondition_0_0")) return false;
    versionSpecifier(builder_, level_ + 1);
    return true;
  }

  // !((without importing)|(importing false))
  private static boolean withImportCondition_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "withImportCondition_0_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_);
    result_ = !withImportCondition_0_1_0(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // (without importing)|(importing false)
  private static boolean withImportCondition_0_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "withImportCondition_0_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = withImportCondition_0_1_0_0(builder_, level_ + 1);
    if (!result_) result_ = withImportCondition_0_1_0_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // without importing
  private static boolean withImportCondition_0_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "withImportCondition_0_1_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokens(builder_, 0, WITHOUT, IMPORTING);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // importing false
  private static boolean withImportCondition_0_1_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "withImportCondition_0_1_0_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokens(builder_, 0, IMPORTING, FALSE);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // with timeout [of] timeoutIntegerExpression (seconds|second) sep
  //                            blockBody?
  //                          end [timeout]
  public static boolean withTimeoutStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "withTimeoutStatement")) return false;
    if (!nextTokenIs(builder_, WITH)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, WITH_TIMEOUT_STATEMENT, null);
    result_ = consumeTokens(builder_, 2, WITH, TIMEOUT);
    pinned_ = result_; // pin = 2
    result_ = result_ && report_error_(builder_, withTimeoutStatement_2(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, timeoutIntegerExpression(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, withTimeoutStatement_4(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, sep(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, withTimeoutStatement_6(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, END)) && result_;
    result_ = pinned_ && withTimeoutStatement_8(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // [of]
  private static boolean withTimeoutStatement_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "withTimeoutStatement_2")) return false;
    consumeToken(builder_, OF);
    return true;
  }

  // seconds|second
  private static boolean withTimeoutStatement_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "withTimeoutStatement_4")) return false;
    boolean result_;
    result_ = consumeToken(builder_, SECONDS);
    if (!result_) result_ = consumeToken(builder_, SECOND);
    return result_;
  }

  // blockBody?
  private static boolean withTimeoutStatement_6(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "withTimeoutStatement_6")) return false;
    blockBody(builder_, level_ + 1);
    return true;
  }

  // [timeout]
  private static boolean withTimeoutStatement_8(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "withTimeoutStatement_8")) return false;
    consumeToken(builder_, TIMEOUT);
    return true;
  }

  /* ********************************************************** */
  // with transaction [sessionSpecificator] sep
  //                                blockBody?
  //                              end [transaction]
  public static boolean withTransactionStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "withTransactionStatement")) return false;
    if (!nextTokenIs(builder_, WITH)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, WITH_TRANSACTION_STATEMENT, null);
    result_ = consumeTokens(builder_, 2, WITH, TRANSACTION);
    pinned_ = result_; // pin = 2
    result_ = result_ && report_error_(builder_, withTransactionStatement_2(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, sep(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, withTransactionStatement_4(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, END)) && result_;
    result_ = pinned_ && withTransactionStatement_6(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // [sessionSpecificator]
  private static boolean withTransactionStatement_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "withTransactionStatement_2")) return false;
    sessionSpecificator(builder_, level_ + 1);
    return true;
  }

  // blockBody?
  private static boolean withTransactionStatement_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "withTransactionStatement_4")) return false;
    blockBody(builder_, level_ + 1);
    return true;
  }

  // [transaction]
  private static boolean withTransactionStatement_6(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "withTransactionStatement_6")) return false;
    consumeToken(builder_, TRANSACTION);
    return true;
  }

  static final Parser BUNDLE_parser_ = (builder_, level_) -> consumeToken(builder_, BUNDLE);
  static final Parser SCRIPTING_ADDITIONS_parser_ = (builder_, level_) -> consumeToken(builder_, SCRIPTING_ADDITIONS);
  static final Parser SCRIPT_parser_ = (builder_, level_) -> consumeToken(builder_, SCRIPT);
}
