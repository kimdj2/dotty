package dotty.dokka

import org.jetbrains.dokka.plugability._
import org.jetbrains.dokka.transformers.sources._

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.model._
import org.jetbrains.dokka.links._
import org.jetbrains.dokka.model.doc._
import org.jetbrains.dokka.base.parsers._
import org.jetbrains.dokka.plugability.DokkaContext
import dokka.java.api._
import collection.JavaConverters._
import org.jetbrains.dokka.model.properties.PropertyContainer
import dotty.dokka.tasty.{DokkaTastyInspector, SbtDokkaTastyInspector}
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.pages._


/** Main Dokka plugin for the doctool.
  *
  * Wires together classes responsible for consuming Tasty and generating
  * documentation.
  *
  * Most of the work of parsing Tasty is done by [[DokkaTastyInspector]].
  */
class DottyDokkaPlugin extends JavaDokkaPlugin:
  override def createSourceToDocumentableTranslator(cxt: DokkaContext, sourceSet: SourceSetWrapper): DModule = cxt.getConfiguration match {
    case dottyConfig: DottyDokkaConfig =>
      val result = dottyConfig.docConfiguration match {
        case DocConfiguration.Standalone(args, tastyFiles) =>
          val inspector = DokkaTastyInspector(sourceSet, new MarkdownParser(null, null, cxt.getLogger), dottyConfig)
          inspector.inspect(args.classpath, tastyFiles)
          inspector.result()
        case DocConfiguration.Sbt(args, tastyFiles, rootCtx) =>
          val inspector =
            SbtDokkaTastyInspector(
              sourceSet,
              //   new MarkdownParser(null, null, cxt.getLogger),
              dottyConfig,
              tastyFiles,
              rootCtx,
            )
          inspector.run()
      }
    
      new DModule(
        sourceSet.getSourceSet.getModuleDisplayName,
        result.asJava,
        Map().asJava,
        null,
        sourceSet.toSet,
        PropertyContainer.Companion.empty()
      )
    case _ =>
      ???
  }

  override def createSignatureProvider(ctcc: CommentsToContentConverter, logger: DokkaLogger) = new ScalaSignatureProvider(ctcc, logger) 
  override def createResourceInstaller(ctx: DokkaContext) = new ScalaResourceInstaller()
  override def createEmbeddedResourceAppender(ctx: DokkaContext) = new ScalaEmbeddedResourceAppender()
  override def createDocumentableToPageTranslator(
        commentsToContentConverter: CommentsToContentConverter,
        signatureProvider: SignatureProvider,
        logger: DokkaLogger
    ) = new ScalaDocumentableToPageTranslator(commentsToContentConverter,signatureProvider, logger)
  override def createPackageHierarchyTransformer(ctx: DokkaContext) = PackageHierarchyTransformer(ctx)
  override def createInheritanceInformationTransformer(ctx: DokkaContext) = InheritanceInformationTransformer(ctx)
  override def createSourceLinksTransformer(
      ctx: DokkaContext,        
      commentsToContentConverter: CommentsToContentConverter,
      signatureProvider: SignatureProvider,
      logger: DokkaLogger
    ) = ScalaSourceLinksTransformer(ctx, commentsToContentConverter, signatureProvider, logger)
  override def createHtmlRenderer(ctx: DokkaContext) = ScalaHtmlRenderer(ctx)
  override def createCommentToContentConverter() = ScalaCommentToContentConverter
  override def createImplicitMembersExtensionTransformer(ctx: DokkaContext) = ImplicitMembersExtensionTransformer(ctx)
