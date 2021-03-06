package groovy.android

import android.content.Context
import android.util.TypedValue
import groovy.android.factory.AndroidFactory
import groovy.android.factory.CollectionFactory
import groovy.android.factory.layout.FrameLayoutFactory
import groovy.android.factory.layout.GridLayoutFactory
import groovy.android.factory.layout.LayoutFactory
import groovy.android.factory.layout.LinearLayoutFactory
import groovy.android.factory.layout.RelativeLayoutFactory
import groovy.android.factory.layout.TableLayoutFactory
import groovy.android.factory.layout.TableRowFactory
import groovy.android.factory.TextViewFactory

import java.util.logging.Logger

class AndroidBuilder extends FactoryBuilderSupport {
    private static final Logger LOG = Logger.getLogger(AndroidBuilder.name)
    public static final String DELEGATE_PROPERTY_OBJECT_ID = "_delegateProperty:id"
    public static final String DEFAULT_DELEGATE_PROPERTY_OBJECT_ID = "id"
    public static final String ANDROID_CONTEXT = "__ANDROID_CONTEXT_"
    public static final String ANDROID_PARENT_CONTEXT = "__ANDROID_PARENT_CONTEXT_"

    protected Context androidContext

    AndroidBuilder(boolean init = true) {
        super(init)
        this[DELEGATE_PROPERTY_OBJECT_ID] = DEFAULT_DELEGATE_PROPERTY_OBJECT_ID
    }

    def registerNodes() {
        registerFactory 'noparent', new CollectionFactory()

        registerFactory 'textView', new TextViewFactory()
    }

    def registerAttributeDelegates() {
        //object id delegate, for propertyNotFound
        addAttributeDelegate(AndroidBuilder.&objectIDAttributeDelegate)

        // listener delegate
        addAttributeDelegate(AndroidFactory.&listenersAttributeDelegate)

        // layoutParams delegate
        addAttributeDelegate(LayoutFactory.&layoutParamsAttributeDelegate)

    }

    def registerLayouts() {
        registerFactory 'relativeLayout', new RelativeLayoutFactory()
        registerFactory 'linearLayout', new LinearLayoutFactory()
        registerFactory 'tableLayout', new TableLayoutFactory()
        registerFactory 'tableRow', new TableRowFactory()
        registerFactory 'frameLayout', new FrameLayoutFactory()
        registerFactory 'gridLayout', new GridLayoutFactory()

    }

    def registerUnitConversion() {
        registerExplicitMethod('dp', 'conversion', this.&convertFrom.curry(TypedValue.COMPLEX_UNIT_DIP))
        registerExplicitMethod('sp', 'conversion', this.&convertFrom.curry(TypedValue.COMPLEX_UNIT_SP))
        registerExplicitMethod('pt', 'conversion', this.&convertFrom.curry(TypedValue.COMPLEX_UNIT_PT))
        registerExplicitMethod('mm', 'conversion', this.&convertFrom.curry(TypedValue.COMPLEX_UNIT_MM))
        registerExplicitMethod('inch', 'conversion', this.&convertFrom.curry(TypedValue.COMPLEX_UNIT_IN))
    }

    public Object build(Context androidContext, Closure c) {
        this.androidContext = androidContext
        c.setDelegate(this)
        return c.call()
    }

    static objectIDAttributeDelegate(def builder, def node, def attributes) {
        def idAttr = builder.getAt(DELEGATE_PROPERTY_OBJECT_ID) ?: DEFAULT_DELEGATE_PROPERTY_OBJECT_ID
        def theID = attributes.remove(idAttr)
        if (theID != null) {
            if (theID instanceof String) {
                builder.setVariable(theID, node)
                if (node) {
                    try {
                        if (node.id == -1) node.id = theID.hashCode()
                    } catch (MissingPropertyException mpe) {
                        // ignore
                    }
                }
            } else if (theID instanceof Number) {
                builder.setVariable("id_${theID.intValue()}".toString(), node)
                if (node) {
                    try {
                        if (node.id == -1) node.id = theID.intValue()
                    } catch (MissingPropertyException mpe) {
                        // ignore
                    }
                }
            }
        }
    }

    Object findPropertyInContextStack(String property) {
        Map<String, Object> context = getContext()
        while (context) {
            if (context.containsKey(property)) {
                return context.get(property)
            } else {
                context = context.get(PARENT_CONTEXT)
            }
        }
        if (property == ANDROID_CONTEXT)
            return androidContext
        return null
    }

    @Override
    protected Object postNodeCompletion(Object parent, Object node) {
        Object result = super.postNodeCompletion(parent, node)
        Object factory = getContextAttribute(CURRENT_FACTORY)
        if (factory instanceof AndroidFactory) {
            return factory.postCompleteNode(this, parent, result)
        }
        return result
    }

    int convertFrom(int unit, Number value) {
        Context context = findPropertyInContextStack(ANDROID_CONTEXT)
        return TypedValue.applyDimension(unit, value.floatValue(), context.resources.displayMetrics).intValue()
    }
}
