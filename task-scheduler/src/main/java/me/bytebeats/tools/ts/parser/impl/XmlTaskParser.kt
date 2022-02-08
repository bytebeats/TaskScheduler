package me.bytebeats.tools.ts.parser.impl

import android.util.Xml
import me.bytebeats.tools.ts.Project
import me.bytebeats.tools.ts.SchedulerLog
import me.bytebeats.tools.ts.Task
import me.bytebeats.tools.ts.TaskScheduler
import me.bytebeats.tools.ts.parser.ITaskParser
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

/**
 * Created by bytebeats on 2022/2/8 : 18:07
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */
object XmlTaskParser : ITaskParser {

    /*******************************启动流程配置文件的节点关键字 */
    private const val NODE_PROJECTS = "projects"
    private const val NODE_PROJECT = "project"
    private const val NODE_TASK = "task"

    /*******************************启动流程配置文件的属性关键字 */
    private const val ATTR_TASK_NAME = "name"
    private const val ATTR_TASK_CLASS = "class"
    private const val ATTR_TASK_PREDECESSOR = "predecessor"
    private const val ATTR_PROJECT_MODE = "mode"
    private const val ATTR_PROCESS_NAME = "process"
    private const val ATTR_THREAD_PRIORITY = "threadPriority"
    private const val ATTR_TASK_PRIORITY = "taskPriority"

    /*******************************启动流程配置文件的属性值关键字 */
    private const val MODE_ALL_PROCESS = "allProcess"
    private const val MODE_MAIN_PROCESS = "mainProcess"
    private const val MODE_SECONDARY_PROCESS = "secondaryProcess"
    private const val PREDECESSOR_SPLITTER = ","


    override fun parse(inputStream: InputStream): List<ITaskParser.ProjectInfo> {
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)
            parser.nextTag()

            val bundles = readProjects(parser)

            val projectInfos = mutableListOf<ITaskParser.ProjectInfo>()

            for (bundle in bundles) {
                val projectInfo = buildProject(bundle)
                projectInfos.add(projectInfo)
            }

            return projectInfos
        } catch (fnfe: FileNotFoundException) {
            SchedulerLog.e(SchedulerLog.TAG, fnfe)
        } catch (xppe: XmlPullParserException) {
            SchedulerLog.e(SchedulerLog.TAG, xppe)
        } catch (ioe: IOException) {
            SchedulerLog.e(SchedulerLog.TAG, ioe)
        }
        return emptyList()
    }

    private fun splitPredecessorIds(predecessorIds: String?): List<String> =
        predecessorIds?.split(PREDECESSOR_SPLITTER) ?: emptyList()

    @Throws(IllegalStateException::class, XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
                else -> {}
            }
        }
    }

    private fun readMode(parser: XmlPullParser): Int {
        return when (parser.getAttributeValue(null, ATTR_PROJECT_MODE)) {
            MODE_MAIN_PROCESS -> TaskScheduler.MAIN_PROCESS_MODE
            MODE_SECONDARY_PROCESS -> TaskScheduler.SECONDARY_PROCESS_MODE
            else -> TaskScheduler.ALL_PROCESS_MODE
        }
    }

    @Throws(IOException::class, XmlPullParserException::class, IllegalArgumentException::class)
    private fun readTask(parser: XmlPullParser): ITaskParser.TaskInfo {
        parser.require(XmlPullParser.START_TAG, null, NODE_TASK)
        val name = parser.getAttributeValue(null, ATTR_TASK_NAME)
        val path = parser.getAttributeValue(null, ATTR_TASK_CLASS)
        val predecessorIds = parser.getAttributeValue(null, ATTR_TASK_PREDECESSOR)
        val threadPriorityStr = parser.getAttributeValue(null, ATTR_THREAD_PRIORITY)
        val taskPriorityStr = parser.getAttributeValue(null, ATTR_TASK_PRIORITY)

        if (name.isNullOrEmpty() || path.isNullOrEmpty()) {
            throw IllegalArgumentException("attribute name or class can't be empty")
        }
        val taskInfo = ITaskParser.TaskInfo(name, path)

        if (!predecessorIds.isNullOrEmpty()) {
            taskInfo.addPredecessors(splitPredecessorIds(predecessorIds))
        }
        if (!threadPriorityStr.isNullOrEmpty()) {
            taskInfo.threadPriority = try {
                threadPriorityStr.toInt()
            } catch (e: NumberFormatException) {
                0
            }
        }
        if (!taskPriorityStr.isNullOrEmpty()) {
            taskInfo.taskPriority = try {
                taskPriorityStr.toInt()
            } catch (e: NumberFormatException) {
                0
            }
        }
        parser.nextTag()
        parser.require(XmlPullParser.END_TAG, null, NODE_TASK)
        return taskInfo
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readProject(parser: XmlPullParser): ITaskParser.TaskBundle {
        parser.require(XmlPullParser.START_TAG, null, NODE_PROJECT)
        val taskInfos = mutableListOf<ITaskParser.TaskInfo>()
        val mode = readMode(parser)
        val processName = parser.getAttributeValue(null, ATTR_PROCESS_NAME)

        while (parser.nextTag() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            } else {
                val name = parser.name
                if (name == NODE_TASK) {
                    taskInfos.add(readTask(parser))
                } else {
                    skip(parser)
                }
            }
        }

        return ITaskParser.TaskBundle(mode, processName, taskInfos)
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readProjects(parser: XmlPullParser): List<ITaskParser.TaskBundle> {
        val projects = mutableListOf<ITaskParser.TaskBundle>()
        parser.require(XmlPullParser.START_TAG, null, NODE_PROJECTS)
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            } else {
                val name = parser.name
                if (name == NODE_PROJECT) {
                    projects.add(readProject(parser))
                } else {
                    skip(parser)
                }
            }
        }
        return projects
    }

    private fun buildProject(bundle: ITaskParser.TaskBundle): ITaskParser.ProjectInfo {
        val taskGraph = mutableMapOf<String, Task>()
        for (info in bundle.taskInfos) {
            val task: Task = try {
                val klass = Class.forName(info.path)
                val t = klass.newInstance() as Task?
                if (info.threadPriority != 0) {
                    t?.threadPriority = info.threadPriority
                }
                if (info.taskPriority != Task.DEFAULT_TASK_PRIORITY) {
                    t?.taskPriority = info.taskPriority
                }
                t
            } catch (cnfe: ClassNotFoundException) {
                SchedulerLog.e(SchedulerLog.TAG, cnfe)
                null
            } catch (ie: InstantiationException) {
                SchedulerLog.e(SchedulerLog.TAG, ie)
                null
            } catch (iae: IllegalAccessException) {
                SchedulerLog.e(SchedulerLog.TAG, iae)
                null
            } ?: throw IllegalArgumentException("Can't create Task ${info.id} from ${info.path}")
            taskGraph[info.id] = task
        }
        val builder = Project.Builder()
        for (info in bundle.taskInfos) {
            val task = taskGraph[info.id]
            builder.add(task!!)
            for (predecessorTask in info.predecessors.map { predecessorName ->
                taskGraph[predecessorName]
                    ?: throw IllegalArgumentException("No such predecessor task $predecessorName")
            }) {
                builder.after(predecessorTask)
            }
        }
        return ITaskParser.ProjectInfo(builder.build(), bundle.mode, bundle.processName)
    }
}