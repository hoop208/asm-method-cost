package com.example.methodcostplugin

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter
import org.objectweb.asm.commons.LocalVariablesSorter

object MethodCostUtil {

    fun trackTime(methodVisitor: MethodVisitor, timeIdentifier: Int,){
        methodVisitor.visitMethodInsn(
            AdviceAdapter.INVOKESTATIC,
            "java/lang/System",
            "currentTimeMillis",
            "()J",
            false
        )
        methodVisitor.visitVarInsn(AdviceAdapter.LSTORE, timeIdentifier)
    }

    fun newParameterArrayList(mv: MethodVisitor, localVariablesSorter: LocalVariablesSorter): Int {
        // new一个ArrayList
        mv.visitTypeInsn(AdviceAdapter.NEW, "java/util/ArrayList")
        mv.visitInsn(AdviceAdapter.DUP)
        mv.visitMethodInsn(
            AdviceAdapter.INVOKESPECIAL,
            "java/util/ArrayList",
            "<init>",
            "()V",
            false
        )
        // 存储new出来的List
        val parametersIdentifier = localVariablesSorter.newLocal(Type.getType(List::class.java))
        mv.visitVarInsn(AdviceAdapter.ASTORE, parametersIdentifier)
        // 返回parametersIdentifier，方便后续访问这个列表
        return parametersIdentifier
    }

    fun fillParameterArray(
        methodDesc: String,
        mv: MethodVisitor,
        parametersIdentifier: Int,
        access: Int
    ) {
        // 判断是不是静态函数
        val isStatic = (access and Opcodes.ACC_STATIC) != 0
        // 静态函数与普通函数的cursor不同
        var cursor = if (isStatic) 0 else 1
        val methodType = Type.getMethodType(methodDesc)
        // 获取参数列表
        methodType.argumentTypes.forEach {
            // 读取列表
            mv.visitVarInsn(AdviceAdapter.ALOAD, parametersIdentifier)
            // 根据不同类型获取不同的指令，比如int是iload, long是lload
            val opcode = it.getOpcode(Opcodes.ILOAD)
            // 通过指令与cursor读取参数的值
            mv.visitVarInsn(opcode, cursor)
            if (it.sort >= Type.BOOLEAN && it.sort <= Type.DOUBLE) {
                // 基本类型转换为包装类型
                typeCastToObject(mv, it)
            }
            // 更新cursor
            cursor += it.size
            // 添加到列表中
            mv.visitMethodInsn(
                AdviceAdapter.INVOKEINTERFACE,
                "java/util/List",
                "add",
                "(Ljava/lang/Object;)Z",
                true
            )
            mv.visitInsn(AdviceAdapter.POP)
        }
    }

    fun trackResult(opcode: Int,mv: MethodVisitor,methodDesc:String,resultIdentifier:Int){
        if ((opcode in AdviceAdapter.IRETURN..AdviceAdapter.RETURN) || opcode == AdviceAdapter.ATHROW) {
            when (opcode) {
                // 基本类型返回
                in AdviceAdapter.IRETURN..AdviceAdapter.DRETURN -> {
                    // 读取返回值
                    loadReturnData(mv, methodDesc)
                }
                // 对象返回
                AdviceAdapter.ARETURN -> {
                    mv.visitInsn(AdviceAdapter.DUP)
                }
                // 空返回
                AdviceAdapter.RETURN -> {
                    mv.visitLdcInsn("void")
                }
                else -> {
                    mv.visitLdcInsn("void")
                }
            }
        } else {
            mv.visitLdcInsn("void")
        }
        mv.visitVarInsn(AdviceAdapter.ASTORE, resultIdentifier)
    }

    fun calculateTimeCost(methodVisitor:MethodVisitor,startIdentifier:Int,endIdentifier:Int,costIdentifier:Int){
        methodVisitor.visitVarInsn(AdviceAdapter.LLOAD, endIdentifier)
        methodVisitor.visitVarInsn(AdviceAdapter.LLOAD, startIdentifier)
        methodVisitor.visitInsn(AdviceAdapter.LSUB)
        methodVisitor.visitVarInsn(AdviceAdapter.LSTORE, costIdentifier)
    }

    private fun loadReturnData(mv: MethodVisitor, methodDesc: String) {
        val methodType = Type.getMethodType(methodDesc)
        if (methodType.returnType.size == 1) {
            mv.visitInsn(AdviceAdapter.DUP)
        } else {
            mv.visitInsn(AdviceAdapter.DUP2)
        }
        typeCastToObject(mv, methodType.returnType)
    }

    private fun typeCastToObject(mv: MethodVisitor, type: Type) {
        when (type) {
            Type.INT_TYPE -> {
                mv.visitMethodInsn(
                    AdviceAdapter.INVOKESTATIC,
                    "java/lang/Integer",
                    "valueOf",
                    "(I)Ljava/lang/Integer;",
                    false
                )
            }
            Type.CHAR_TYPE -> {
                mv.visitMethodInsn(
                    AdviceAdapter.INVOKESTATIC,
                    "java/lang/Character",
                    "valueOf",
                    "(C)Ljava/lang/Character;",
                    false
                )
            }
            Type.BYTE_TYPE -> {
                mv.visitMethodInsn(
                    AdviceAdapter.INVOKESTATIC,
                    "java/lang/Byte",
                    "valueOf",
                    "(B)Ljava/lang/Byte;",
                    false
                )
            }
            Type.BOOLEAN_TYPE -> {
                mv.visitMethodInsn(
                    AdviceAdapter.INVOKESTATIC,
                    "java/lang/Boolean",
                    "valueOf",
                    "(Z)Ljava/lang/Boolean;",
                    false
                )
            }
            Type.SHORT_TYPE -> {
                mv.visitMethodInsn(
                    AdviceAdapter.INVOKESTATIC,
                    "java/lang/Short",
                    "valueOf",
                    "(S)Ljava/lang/Short;",
                    false
                )
            }
            Type.FLOAT_TYPE -> {
                mv.visitMethodInsn(
                    AdviceAdapter.INVOKESTATIC,
                    "java/lang/Float",
                    "valueOf",
                    "(F)Ljava/lang/Float;",
                    false
                )
            }
            Type.LONG_TYPE -> {
                mv.visitMethodInsn(
                    AdviceAdapter.INVOKESTATIC,
                    "java/lang/Long",
                    "valueOf",
                    "(J)Ljava/lang/Long;",
                    false
                )
            }
            Type.DOUBLE_TYPE -> {
                mv.visitMethodInsn(
                    AdviceAdapter.INVOKESTATIC,
                    "java/lang/Double",
                    "valueOf",
                    "(D)Ljava/lang/Double;",
                    false
                )
            }
        }
    }

}
