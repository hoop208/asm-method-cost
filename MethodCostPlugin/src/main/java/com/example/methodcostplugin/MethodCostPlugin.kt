package com.example.methodcostplugin

import com.android.build.api.instrumentation.*
import com.android.build.api.variant.AndroidComponentsExtension
import com.google.gson.Gson
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter
import org.objectweb.asm.util.TraceClassVisitor
import java.io.PrintWriter

class MethodCostPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.extensions.add("MethodCost", MethodCostExtension::class.java)
        val config = (project.extensions.findByName("MethodCost") as? MethodCostExtension)
            ?: MethodCostExtension()

        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)

        androidComponents.onVariants { variant ->
            println("MethodCost(logImplClass=${config.logImplClass},configFilePath=${config.configFilePath};enabled=${config.enabled},targetBuildType=${config.targetBuildType},traceClass=${config.traceClass})")
            val fileInput = project.file(config.configFilePath).readText()
            println("MethodCost(fileInput=$fileInput)")
            val methodCostFilter = kotlin.runCatching {
                Gson().fromJson(fileInput, MethodCostFilter::class.java)
            }.getOrNull() ?: MethodCostFilter()
            val buildType = variant.buildType
            println("variant(buildType=$buildType)")

            if (config.enabled && buildType.equals(config.targetBuildType, true)) {
                variant.instrumentation.transformClassesWith(
                    MethodCostVisitorFactory::class.java,
                    InstrumentationScope.ALL
                ) {
                    it.logImplClass.set(config.logImplClass)
                    it.filter.set(methodCostFilter)
                    it.traceClass.set(config.traceClass)
                }
                variant.instrumentation.setAsmFramesComputationMode(FramesComputationMode.COPY_FRAMES)
            }
        }

    }

    interface MethodVisitorParams : InstrumentationParameters {

        @get:Input
        val logImplClass: Property<String>

        @get:Input
        val filter: Property<MethodCostFilter>

        @get:Input
        val traceClass: Property<Boolean>
    }

    abstract class MethodCostVisitorFactory :
        AsmClassVisitorFactory<MethodVisitorParams> {
        override fun createClassVisitor(
            classContext: ClassContext,
            nextClassVisitor: ClassVisitor
        ): ClassVisitor {
            val className = classContext.currentClassData.className

            val logImplClass = parameters.get().logImplClass.get()
            val traceClass = parameters.get().traceClass.get()
            val methodNameList = parameters.get().filter.get().methodNameList
            return if (traceClass) {
                val tracedNextVisitor = TraceClassVisitor(nextClassVisitor, PrintWriter(System.out))
                MethodCostClassVisitor(
                    tracedNextVisitor,
                    logImplClass = logImplClass,
                    className = className,
                    methodNameList = methodNameList
                )
            } else {
                MethodCostClassVisitor(
                    nextClassVisitor,
                    logImplClass = logImplClass,
                    className = className,
                    methodNameList = methodNameList
                )
            }
        }

        override fun isInstrumentable(classData: ClassData): Boolean {
            val traceClasses = parameters.get().filter.get().classNameList
            return traceClasses.contains(classData.className)
        }
    }

    class MethodCostClassVisitor(
        nextVisitor: ClassVisitor,
        val logImplClass: String,
        val className: String,
        val methodNameList: List<String>,
    ) :
        ClassVisitor(Opcodes.ASM5, nextVisitor) {

        var start: Int = 0
        var end: Int = 0
        var cost: Int = 0

        override fun visitMethod(
            access: Int,
            name: String?,
            descriptor: String?,
            signature: String?,
            exceptions: Array<out String>?
        ): MethodVisitor {
            val methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions)
            val newMethodVisitor =
                object : AdviceAdapter(Opcodes.ASM5, methodVisitor, access, name, descriptor) {

                    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
                        super.visitMaxs(maxStack + 4, maxLocals)
                    }

                    @Override
                    override fun onMethodEnter() {
                        // 方法开始
                        if (isNeedVisitMethod(name)) {
                            start = newLocal(Type.LONG_TYPE)
                            MethodCostUtil.trackTime(
                                methodVisitor = methodVisitor,
                                timeIdentifier = start
                            )
                        }
                        super.onMethodEnter()
                    }

                    @Override
                    override fun onMethodExit(opcode: Int) {
                        // 方法结束
                        if (isNeedVisitMethod(name)) {

                            //读取输入
                            val parametersIdentifier =
                                MethodCostUtil.newParameterArrayList(mv, this)
                            MethodCostUtil.fillParameterArray(
                                methodDesc, mv, parametersIdentifier, access
                            )

                            //读取输出
                            val resultIdentifier = newLocal(Type.getType(Object::class.java))
                            MethodCostUtil.trackResult(
                                opcode = opcode,
                                mv = mv,
                                methodDesc = methodDesc,
                                resultIdentifier = resultIdentifier
                            )

                            end = newLocal(Type.LONG_TYPE)
                            MethodCostUtil.trackTime(
                                methodVisitor = methodVisitor,
                                timeIdentifier = end
                            )

                            cost = newLocal(Type.LONG_TYPE)
                            MethodCostUtil.calculateTimeCost(
                                methodVisitor = methodVisitor,
                                startIdentifier = start,
                                endIdentifier = end,
                                costIdentifier = cost
                            )

                            methodVisitor.visitLdcInsn(className)
                            methodVisitor.visitLdcInsn(name)
                            methodVisitor.visitVarInsn(LLOAD, cost)
                            methodVisitor.visitVarInsn(ALOAD, parametersIdentifier)
                            methodVisitor.visitVarInsn(ALOAD, resultIdentifier)

                            methodVisitor.visitMethodInsn(
                                INVOKESTATIC,
                                logImplClass,
                                "log",
                                "(Ljava/lang/String;Ljava/lang/String;JLjava/util/List;Ljava/lang/Object;)V",
                                false
                            )
                        }
                        super.onMethodExit(opcode)
                    }
                }
            return newMethodVisitor
        }

        private fun isNeedVisitMethod(name: String?): Boolean {
            return methodNameList.contains(name)
        }
    }


}