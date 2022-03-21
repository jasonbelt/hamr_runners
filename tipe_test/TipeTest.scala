// #Sireum
package org.sireum.cli.hamr_runners.tipe_test

import org.sireum._
import org.sireum.lang.ast._
import org.sireum.lang.ast.Exp.{Binary, LitB, LitF32, LitZ}
import org.sireum.lang.symbol.{Info, Scope, TypeInfo}
import org.sireum.lang.{ast => AST}
import org.sireum.lang.tipe._
import org.sireum.lang.symbol.Resolver._
import org.sireum.lang.symbol.Scope.Local
import org.sireum.message.{Position, Reporter, ReporterImpl}

object TipeTest extends App {

  def t1(): B = {
    val nameMap: NameMap = HashMap.empty
    val typeMap: TypeMap = HashMap.empty
    val poset: Poset[QName] = Poset.empty
    val aliases: HashMap[QName, AST.Typed] = HashMap.empty

    val emptyTypeHierarchy: TypeHierarchy = TypeHierarchy(nameMap, typeMap, poset, aliases)

    val context: QName = ISZ("a")

    val isInMutableContext: B = F
    val strictAliasing: B = F

    val mode: TypeChecker.ModeContext.Type = TypeChecker.ModeContext.Code

    val scopeNameMap: HashMap[String, Info] = HashMap.empty
    val scopeTypeMap: HashMap[String, TypeInfo] = HashMap.empty
    val localThisOp: Option[AST.Typed] = None()
    val methodReturnOpt: Option[AST.Typed] = None()
    val indexMap: HashMap[String, AST.Typed] = HashMap.empty
    val outerOpt: Option[Scope] = None()

    val emptyScope: Scope = Local(scopeNameMap, scopeTypeMap, localThisOp, methodReturnOpt, indexMap, outerOpt)

    val noPosOpt: Option[Position] = None()
    val emptyAttr: Attr = Attr(noPosOpt)

    val emptyRAttr = ResolvedAttr(posOpt = None(), resOpt = None(), typedOpt = None())

    val xF = LitB(value = F, attr = emptyAttr)
    val xT = LitB(value = T, attr = emptyAttr)

    val bExp: AST.Exp =
      Binary(left = xF, op = "&", right = xT, emptyRAttr)

    val z1 = LitZ(value = 1, attr = emptyAttr)
    val f1 = LitF32(value = 1f, attr = emptyAttr)

    val iExp: AST.Exp =
      Binary(left = z1, op = "+", right = z1, emptyRAttr)


    def test(e: AST.Exp, expectedType: Option[AST.Typed], th: TypeHierarchy, scope: Scope): B = {
      val typeChecker: TypeChecker = TypeChecker(th, context, isInMutableContext, mode, strictAliasing)

      val reporter: Reporter = ReporterImpl(ISZ())

      val (rexp, opt): (AST.Exp, Option[AST.Typed]) = typeChecker.checkExp(expectedType, scope, e, reporter)

      println("------------------")

      println(s"exp = $e")
      println(s"opt = $opt")
      reporter.printMessages()

      return reporter.hasError
    }

    test(bExp, Some(Typed.b), emptyTypeHierarchy, emptyScope)

    test(iExp, Some(Typed.z), emptyTypeHierarchy, emptyScope)

    {
      val id: Id = Id(value = "boolVarRef", emptyAttr)

      var scopeNameMap: HashMap[String, Info] = HashMap.empty
      val symbol = Info.LocalVar(name = ISZ("boolVarRef"), isVal = T, ast = id, typedOpt = Some(Typed.b), resOpt = None())
      scopeNameMap = scopeNameMap + ("boolVarRef" ~> symbol)

      val scope = Local(scopeNameMap, scopeTypeMap, localThisOp, methodReturnOpt, indexMap, outerOpt)

      val selectExp: Exp.Select = Exp.Select(receiverOpt = None(), id = id, targs = ISZ(), emptyRAttr)

      val bExp: AST.Exp = Binary(left = xF, op = "&", right = selectExp, emptyRAttr)

      test(bExp, Some(Typed.b), emptyTypeHierarchy, scope)
    }

    {

      val id: Id = Id(value = "enumValue", emptyAttr)

    }

    return T
  }

  def main(args: ISZ[String]): Z = {
    t1()
    return 0
  }
}
