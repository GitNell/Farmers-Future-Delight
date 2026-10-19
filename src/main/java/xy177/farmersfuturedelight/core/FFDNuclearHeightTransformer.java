package xy177.farmersfuturedelight.core;

import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

final class FFDNuclearHeightTransformer {
    private static final String HEIGHT = "xy177/farmersfuturedelight/core/FFDHeightHooks";
    private static final String HOOKS = "xy177/farmersfuturedelight/core/FFDNuclearHeightHooks";
    private static final String WORLD = "Lnet/minecraft/world/World;";
    private static final String CHUNK = "Lnet/minecraft/world/chunk/Chunk;";
    private static final String RADIATION = "com/hbm/handler/RadiationSystemNT";
    private static final String STORAGE = RADIATION + "$ChunkRadiationStorage";

    private FFDNuclearHeightTransformer() {
    }

    static boolean supports(String name) {
        return "ic2.core.ExplosionIC2".equals(name)
                || "com.hbm.lib.Library".equals(name)
                || "com.hbm.explosion.ExplosionNukeRayBatched".equals(name)
                || RADIATION.replace('/', '.').equals(name)
                || STORAGE.replace('/', '.').equals(name);
    }

    static byte[] transform(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        switch (node.name) {
            case "com/hbm/lib/Library":
                hbmBlockTrace(node);
                break;
            case "ic2/core/ExplosionIC2":
                ic2(node);
                break;
            case "com/hbm/explosion/ExplosionNukeRayBatched":
                hbmExplosion(node);
                break;
            case RADIATION:
                radiation(node);
                break;
            case STORAGE:
                radiationStorage(node);
                break;
            default:
                throw new IllegalStateException("Unexpected nuclear height target " + node.name);
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        LogManager.getLogger("FFD Height Core").info(
                "Patched nuclear height compatibility for {}", node.name);
        return writer.toByteArray();
    }

    private static void hbmBlockTrace(ClassNode node) {
        int methods = 0;
        int bounds = 0;
        int storage = 0;
        for (MethodNode method : node.methods) {
            if (!"rayTraceBlocksInternal".equals(method.name)
                    || !("(" + WORLD + "Lnet/minecraft/util/math/Vec3d;"
                            + "Lnet/minecraft/util/math/Vec3d;ZZZI)"
                            + "Lnet/minecraft/util/math/RayTraceResult;").equals(method.desc)) {
                continue;
            }
            require((method.access & Opcodes.ACC_STATIC) != 0, "HBM block trace static method");
            methods++;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (constant(instruction, -256)) {
                    AbstractInsnNode value = previous(instruction);
                    AbstractInsnNode mask = next(instruction);
                    AbstractInsnNode branch = next(mask);
                    require(value instanceof VarInsnNode && value.getOpcode() == Opcodes.ILOAD
                            && mask != null && mask.getOpcode() == Opcodes.IAND
                            && branch != null && (branch.getOpcode() == Opcodes.IFEQ
                                || branch.getOpcode() == Opcodes.IFNE), "HBM block trace height mask");
                    method.instructions.remove(instruction);
                    InsnList height = load(0);
                    height.add(call(HOOKS, "hbmTraceOutsideHeight", "(I" + WORLD + ")Z"));
                    replace(method, mask, height);
                    bounds++;
                } else if (instruction.getOpcode() == Opcodes.AALOAD) {
                    AbstractInsnNode index = previous(instruction);
                    AbstractInsnNode array = previous(index);
                    if (!(index instanceof VarInsnNode) || index.getOpcode() != Opcodes.ILOAD
                            || !(array instanceof VarInsnNode) || array.getOpcode() != Opcodes.ALOAD) {
                        continue;
                    }
                    InsnList section = load(0);
                    section.add(call(HOOKS, "hbmTraceStorage",
                            "([Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;I"
                                    + WORLD + ")Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;"));
                    replace(method, instruction, section);
                    storage++;
                }
            }
        }
        require(methods == 1 && bounds == 2 && storage == 2,
                "HBM block trace height patterns " + methods + "/" + bounds + "/" + storage);
    }

    private static void ic2(ClassNode node) {
        Supplier<InsnList> world = () -> fieldWorld(node.name, "worldObj");
        int constructorHeight = 0;
        int allocation = 0;
        int indices = 0;
        int loop = 0;
        int lower = 0;
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if ("<init>".equals(method.name) && "ic2/core/IC2".equals(call.owner)
                            && "getWorldHeight".equals(call.name)
                            && ("(" + WORLD + ")I").equals(call.desc)) {
                        InsnList extra = new InsnList();
                        extra.add(new VarInsnNode(Opcodes.ALOAD, 1));
                        extra.add(call(HOOKS, "ic2ExplosionHeight", "(I" + WORLD + ")I"));
                        method.instructions.insert(call, extra);
                        constructorHeight++;
                    }
                }
                if (instruction instanceof FieldInsnNode) {
                    FieldInsnNode field = (FieldInsnNode) instruction;
                    AbstractInsnNode next = next(field);
                    if (field.getOpcode() == Opcodes.GETFIELD
                            && node.name.equals(field.owner) && "mapHeight".equals(field.name)
                            && next instanceof TypeInsnNode
                            && next.getOpcode() == Opcodes.ANEWARRAY
                            && "[J".equals(((TypeInsnNode) next).desc)) {
                        InsnList extra = bound(world, "minY");
                        extra.add(new InsnNode(Opcodes.ISUB));
                        method.instructions.insert(field, extra);
                        allocation++;
                    }
                    if (field.getOpcode() != Opcodes.GETFIELD
                            || !node.name.equals(field.owner)
                            || !"destroyedBlockPositions".equals(field.name)) {
                        continue;
                    }
                    if (next.getOpcode() == Opcodes.ARRAYLENGTH) {
                        AbstractInsnNode variable = previous(previous(field));
                        require(variable instanceof VarInsnNode
                                && variable.getOpcode() == Opcodes.ILOAD, "IC2 destruction loop");
                        int index = ((VarInsnNode) variable).var;
                        AbstractInsnNode store = previous(variable);
                        while (store != null && !(store instanceof VarInsnNode
                                && store.getOpcode() == Opcodes.ISTORE
                                && ((VarInsnNode) store).var == index)) {
                            store = previous(store);
                        }
                        require(store != null && constant(previous(store), 0),
                                "IC2 destruction loop origin");
                        replace(method, previous(store), bound(world, "minY"));
                        InsnList extra = bound(world, "minY");
                        extra.add(new InsnNode(Opcodes.IADD));
                        method.instructions.insert(next, extra);
                        loop++;
                    } else if (next instanceof VarInsnNode
                            && next.getOpcode() == Opcodes.ILOAD) {
                        InsnList extra = bound(world, "minY");
                        extra.add(new InsnNode(Opcodes.ISUB));
                        method.instructions.insert(next, extra);
                        indices++;
                    }
                }
                if ("shootRay".equals(method.name) && instruction.getOpcode() == Opcodes.IFLT) {
                    AbstractInsnNode after = next(instruction);
                    AbstractInsnNode height = next(next(after));
                    if (after instanceof VarInsnNode && height instanceof FieldInsnNode
                            && "mapHeight".equals(((FieldInsnNode) height).name)) {
                        method.instructions.insertBefore(instruction, bound(world, "minY"));
                        ((JumpInsnNode) instruction).setOpcode(Opcodes.IF_ICMPLT);
                        lower++;
                    }
                }
            }
        }
        require(constructorHeight == 1 && allocation == 1 && indices == 3
                && loop == 1 && lower == 1, "IC2 explosion height patterns "
                        + constructorHeight + "/" + allocation + "/" + indices
                        + "/" + loop + "/" + lower);
    }

    private static void hbmExplosion(ClassNode node) {
        Supplier<InsnList> world = () -> fieldWorld(node.name, "world");
        int bounds = 0;
        int encode = 0;
        int decode = 0;
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if ("collectTip".equals(method.name) || "cacheChunksTick".equals(method.name)) {
                    if (instruction.getOpcode() == Opcodes.IFLT
                            && previous(instruction) instanceof VarInsnNode
                            && previous(instruction).getOpcode() == Opcodes.ILOAD
                            && constant(next(next(instruction)), 255)) {
                        method.instructions.insertBefore(instruction, bound(world, "minY"));
                        ((JumpInsnNode) instruction).setOpcode(Opcodes.IF_ICMPLT);
                        bounds++;
                    } else if (constant(instruction, 255)
                            && next(instruction).getOpcode() == Opcodes.IF_ICMPLE) {
                        replace(method, instruction, bound(world, "maxYInclusive"));
                        bounds++;
                    }
                } else if ("addPos".equals(method.name) && constant(instruction, 255)) {
                    AbstractInsnNode height = next(instruction);
                    AbstractInsnNode subtract = next(height);
                    require(height instanceof VarInsnNode
                            && ((VarInsnNode) height).var == 2
                            && subtract.getOpcode() == Opcodes.ISUB, "HBM explosion Y encoding");
                    method.instructions.remove(instruction);
                    method.instructions.set(subtract, call(HOOKS, "explosionYIndex", "(I)I"));
                    encode++;
                } else if ("processChunkBlocks".equals(method.name) && constant(instruction, 255)) {
                    AbstractInsnNode subtract = next(instruction);
                    while (subtract != null && subtract.getOpcode() != Opcodes.ISUB) {
                        subtract = next(subtract);
                    }
                    require(subtract != null && previous(subtract).getOpcode() == Opcodes.ISHR,
                            "HBM explosion Y decoding");
                    method.instructions.remove(instruction);
                    method.instructions.set(subtract, call(HOOKS, "explosionYFromIndex", "(I)I"));
                    decode++;
                }
            }
        }
        require(bounds == 2 && encode == 1 && decode == 1,
                "HBM explosion height patterns " + bounds + "/" + encode + "/" + decode);
    }

    private static void radiation(ClassNode node) {
        int bounds = 0;
        int storage = 0;
        for (MethodNode method : node.methods) {
            Supplier<InsnList> world;
            if ("incrementRad".equals(method.name) || "decrementRad".equals(method.name)
                    || "isSubChunkLoaded".equals(method.name)) {
                world = () -> load(0);
            } else if ("buildPocket".equals(method.name)) {
                world = () -> load(1);
            } else if ("updateRadiation".equals(method.name)) {
                world = radiationWorld(method);
            } else {
                world = null;
            }
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (world != null && isGetY(previous(instruction))) {
                    if (instruction.getOpcode() == Opcodes.IFLT
                            || instruction.getOpcode() == Opcodes.IFGE) {
                        method.instructions.insertBefore(instruction, bound(world, "minY"));
                        ((JumpInsnNode) instruction).setOpcode(
                                instruction.getOpcode() == Opcodes.IFLT
                                        ? Opcodes.IF_ICMPLT : Opcodes.IF_ICMPGE);
                        bounds++;
                    } else if (constant(instruction, 255)) {
                        replace(method, instruction, bound(world, "maxYInclusive"));
                        bounds++;
                    }
                }
                if ("rebuildChunkPockets".equals(method.name)
                        && instruction instanceof MethodInsnNode) {
                    MethodInsnNode access = (MethodInsnNode) instruction;
                    if (!"net/minecraft/world/chunk/Chunk".equals(access.owner)
                            || !("func_76587_i".equals(access.name)
                                || "getBlockStorageArray".equals(access.name))) {
                        continue;
                    }
                    AbstractInsnNode index = next(access);
                    AbstractInsnNode read = next(index);
                    require(index instanceof VarInsnNode && ((VarInsnNode) index).var == 1
                            && read.getOpcode() == Opcodes.AALOAD, "HBM radiation chunk storage");
                    method.instructions.remove(access);
                    method.instructions.set(read, call(HEIGHT, "getStorageForSectionY",
                            "(" + CHUNK + "I)Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;"));
                    storage++;
                }
            }
        }
        require(bounds == 10 && storage == 1,
                "HBM radiation height patterns " + bounds + "/" + storage);
    }

    private static void radiationStorage(ClassNode node) {
        int allocation = 0;
        int indices = 0;
        int guard = 0;
        int marker = 0;
        int buffer = 0;
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if ("<init>".equals(method.name) && constant(instruction, 16)
                        && next(instruction).getOpcode() == Opcodes.ANEWARRAY) {
                    InsnList size = load(2);
                    size.add(call(HOOKS, "radiationSectionCount", "(" + CHUNK + ")I"));
                    replace(method, instruction, size);
                    allocation++;
                }
                if (("getForYLevel".equals(method.name) || "setForYLevel".equals(method.name))
                        && instruction.getOpcode() == Opcodes.ISHR
                        && constant(previous(instruction), 4)) {
                    AbstractInsnNode value = previous(previous(instruction));
                    require(value instanceof VarInsnNode && ((VarInsnNode) value).var == 1,
                            "HBM radiation Y index");
                    method.instructions.remove(previous(instruction));
                    InsnList index = load(0);
                    index.add(new FieldInsnNode(Opcodes.GETFIELD, node.name, "chunk", CHUNK));
                    index.add(call(HOOKS, "radiationIndex", "(I" + CHUNK + ")I"));
                    replace(method, instruction, index);
                    indices++;
                }
                if ("getForYLevel".equals(method.name)
                        && (instruction.getOpcode() == Opcodes.IF_ICMPGT
                            || instruction.getOpcode() == Opcodes.IF_ICMPLE)
                        && previous(instruction).getOpcode() == Opcodes.ARRAYLENGTH) {
                    ((JumpInsnNode) instruction).setOpcode(
                            instruction.getOpcode() == Opcodes.IF_ICMPGT
                                    ? Opcodes.IF_ICMPGE : Opcodes.IF_ICMPLT);
                    guard++;
                }
                if ("readFromNBT".equals(method.name) && marker == 0
                        && instruction instanceof MethodInsnNode) {
                    MethodInsnNode read = (MethodInsnNode) instruction;
                    if ("java/nio/ByteBuffer".equals(read.owner)
                            && "get".equals(read.name) && "()B".equals(read.desc)) {
                        method.instructions.set(read, call(HOOKS, "readRadiationSectionMarker",
                                "(Ljava/nio/ByteBuffer;)B"));
                        marker++;
                    }
                }
                if ("<clinit>".equals(method.name) && constant(instruction, 524288)) {
                    method.instructions.set(instruction, new LdcInsnNode(786432));
                    buffer++;
                }
            }
        }
        require(allocation == 1 && indices == 6 && guard == 1 && marker == 1 && buffer == 1,
                "HBM radiation storage patterns " + allocation + "/" + indices + "/"
                        + guard + "/" + marker + "/" + buffer);
    }

    private static InsnList fieldWorld(String owner, String field) {
        InsnList instructions = load(0);
        instructions.add(new FieldInsnNode(Opcodes.GETFIELD, owner, field, WORLD));
        return instructions;
    }

    private static Supplier<InsnList> radiationWorld(MethodNode method) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (!(instruction instanceof FieldInsnNode)) {
                continue;
            }
            FieldInsnNode field = (FieldInsnNode) instruction;
            AbstractInsnNode source = previous(field);
            if (field.getOpcode() == Opcodes.GETFIELD
                    && (RADIATION + "$WorldRadiationData").equals(field.owner)
                    && "world".equals(field.name) && WORLD.equals(field.desc)
                    && source instanceof VarInsnNode && source.getOpcode() == Opcodes.ALOAD) {
                int variable = ((VarInsnNode) source).var;
                return () -> {
                    InsnList instructions = load(variable);
                    instructions.add(new FieldInsnNode(Opcodes.GETFIELD,
                            field.owner, field.name, field.desc));
                    return instructions;
                };
            }
        }
        throw new IllegalStateException("HBM active radiation world");
    }

    private static InsnList load(int variable) {
        InsnList instructions = new InsnList();
        instructions.add(new VarInsnNode(Opcodes.ALOAD, variable));
        return instructions;
    }

    private static InsnList bound(Supplier<InsnList> world, String method) {
        InsnList instructions = world.get();
        instructions.add(call(HEIGHT, method, "(" + WORLD + ")I"));
        return instructions;
    }

    private static MethodInsnNode call(String owner, String method, String descriptor) {
        return new MethodInsnNode(Opcodes.INVOKESTATIC, owner, method, descriptor, false);
    }

    private static void replace(MethodNode method, AbstractInsnNode old, InsnList instructions) {
        method.instructions.insertBefore(old, instructions);
        method.instructions.remove(old);
    }

    private static AbstractInsnNode next(AbstractInsnNode instruction) {
        do {
            instruction = instruction.getNext();
        } while (instruction != null && instruction.getOpcode() < 0);
        return instruction;
    }

    private static AbstractInsnNode previous(AbstractInsnNode instruction) {
        if (instruction == null) {
            return null;
        }
        do {
            instruction = instruction.getPrevious();
        } while (instruction != null && instruction.getOpcode() < 0);
        return instruction;
    }

    private static boolean isGetY(AbstractInsnNode instruction) {
        if (!(instruction instanceof MethodInsnNode)) {
            return false;
        }
        MethodInsnNode call = (MethodInsnNode) instruction;
        return "()I".equals(call.desc)
                && ("func_177956_o".equals(call.name) || "getY".equals(call.name));
    }

    private static boolean constant(AbstractInsnNode instruction, int value) {
        if (instruction == null) {
            return false;
        }
        if (instruction instanceof IntInsnNode) {
            return ((IntInsnNode) instruction).operand == value;
        }
        if (instruction instanceof LdcInsnNode) {
            return Integer.valueOf(value).equals(((LdcInsnNode) instruction).cst);
        }
        return value >= -1 && value <= 5
                && instruction.getOpcode() == Opcodes.ICONST_0 + value;
    }

    private static void require(boolean condition, String description) {
        if (!condition) {
            throw new IllegalStateException(description);
        }
    }
}
