package de.atennert.lcarswm.drawing

import de.atennert.lcarswm.system.DrawApiDummy
import de.atennert.lcarswm.system.api.DrawApi
import kotlinx.cinterop.*
import xlib.*
import kotlin.test.*

class ColorFactoryTest {

    @AfterTest
    fun teardown() {
        drawApi.clear()
    }

    @Test
    fun `create color GCs`() {
        val colorFactory = ColorFactory(drawApi, screen)
        val color = Color(0xFFFF, 0x9999, 0)

        val gc = colorFactory.createColorGC(1.convert(), color)
        assertNotNull(gc)
        assertEquals(1.toULong(), drawApi.pixel)
    }

    @Test
    fun `create two color GCs`() {
        val colorFactory = ColorFactory(drawApi, screen)
        val color1 = Color(0xFFFF, 0x9999, 0)
        val color2 = Color(0xFFFF, 0, 0x9999)

        val gc1 = colorFactory.createColorGC(1.convert(), color1)
        val gc2 = colorFactory.createColorGC(1.convert(), color2)

        assertNotNull(gc1)
        assertNotNull(gc2)
        assertEquals(2.toULong(), drawApi.pixel)
    }

    @Test
    fun `allocate a color only once`() {
        val colorFactory = ColorFactory(drawApi, screen)
        val color = Color(0xFFFF, 0x9999, 0)

        val gc1 = colorFactory.createColorGC(1.convert(), color)
        val gc2 = colorFactory.createColorGC(1.convert(), color)

        assertNotNull(gc1)
        assertEquals(gc1, gc2)
        assertEquals(1.toULong(), drawApi.pixel)
    }

    @Test
    fun `create different GCs for different drawables`() {
        val colorFactory = ColorFactory(drawApi, screen)
        val color = Color(0xFFFF, 0x9999, 0)

        val gc1 = colorFactory.createColorGC(1.convert(), color)
        val gc2 = colorFactory.createColorGC(2.convert(), color)

        assertNotNull(gc1)
        assertNotNull(gc2)
        assertNotEquals(gc1, gc2)
        assertEquals(1.toULong(), drawApi.pixel)
    }

    private val drawApi = object : DrawApiDummy() {
        val gcs = mutableListOf<GC>()

        var pixel = 0.toULong()

        fun clear() {
            pixel = 0.convert()
            gcs.forEach { nativeHeap.free(it) }
            gcs.clear()
        }

        override fun createColormap(window: Window, visual: CValuesRef<Visual>, alloc: Int): Colormap {
            return 42.convert()
        }

        override fun allocColor(colorMap: Colormap, color: CPointer<XColor>): Int {
            assertEquals(42.toULong(), colorMap)
            pixel += 1.convert()
            color.pointed.pixel = pixel
            return 1
        }

        override fun createGC(drawable: Drawable, mask: ULong, gcValues: CValuesRef<XGCValues>?): GC? {
            val gc: GC = nativeHeap.alloc<GCVar>().ptr.reinterpret()
            gcs.add(gc)
            return gc
        }
    }

    @ThreadLocal
    companion object {
        private lateinit var visual: Visual
        private lateinit var screen: Screen

        @BeforeClass
        fun setupAll() {
            visual = nativeHeap.alloc()
            screen = nativeHeap.alloc()
            screen.root = 1.convert()
            screen.root_visual = visual.ptr
        }

        @AfterClass
        fun teardownAll() {
            nativeHeap.free(screen)
            nativeHeap.free(visual)
        }
    }
}