#include <stdlib.h>
#include <jni.h>

#define PATH_SEPARATOR ":" /* define it to be ':' on Solaris */

#define USER_CLASSPATH "bin/Transcriber.jar"
#define TRANSCRIBER_CLASS "demo/sphinx/transcriber/Transcriber"


JavaVM* S4JniExample_createJVM(){
	JNIEnv *env;
    JavaVM *vm;
    jint res;

//    setenv("JAVA_VM_VERSION", "1.5", 1);
//    setenv("JAVA_VM_VERSION", "1.5", 0);
    JavaVMInitArgs vm_args;
    JavaVMOption options[7];

    options[0].optionString = malloc(3000*sizeof(char));
    sprintf(options[0].optionString, "-Djava.class.path=../classes:"USER_CLASSPATH);
    options[1].optionString = "-Xmx1024m";

	int enableRemDebugging = 1;
	if(enableRemDebugging){
	    printf("enable remote debugging");

		// intellij remote debugging support
		options[2].optionString = "-Xdebug";
		options[3].optionString = "-Xnoagent";
		options[4].optionString = "-Djava.compiler=NONE";
		options[5].optionString = "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=34343";

		vm_args.nOptions = 6;
	}else{
		vm_args.nOptions = 2;
	}

	vm_args.options = options;
    vm_args.version = 0x00010002;

    vm_args.ignoreUnrecognized = JNI_TRUE;
    /* Create the Java VM */

    res = JNI_CreateJavaVM(&vm, (void**)&env, &vm_args);

	return vm;
}

/*!
 * \brief Shuts down an existing java virtual machine.
 * \param jvm Pointer to the jvm to be destroyed.
 *
 * This method is used to shut down an existing java virtual machine.
 */
int S4JniExample_destroyJVM(JavaVM *jvm){
//	JNIEnv *env = getAttachedEnv(self, jvm);
//
//	if ((*env)->ExceptionOccurred(env)) {
//        (*env)->ExceptionDescribe(env);
//    }

	 // detach the current thread from the vm
    (*jvm)->DetachCurrentThread(jvm);

    (*jvm)->DestroyJavaVM(jvm);

	return 0;
}


/*!
 * \brief Attaches the current thread to a given vm instance
 * \param self Pointer to bbcm-instance
 * \param jvm Pointer to the jvm use for attachment
 *
 * This method is used to attach the current thread to a given vm instance
 */
JNIEnv* getAttachedEnv(JavaVM *jvm){
	JNIEnv *localEnv = NULL;
	int envErr = 0;

	/* get a local java env */
	envErr = (*jvm)->AttachCurrentThread( jvm, (void**)&localEnv, NULL );

	if ( envErr != 0 ){
		if ( (*localEnv)->ExceptionCheck( localEnv ) == JNI_TRUE ){
		  (*localEnv)->ExceptionDescribe( localEnv );
		}
	}

	if((*localEnv)->ExceptionOccurred(localEnv)){
	    (*localEnv)->ExceptionDescribe(localEnv);
    }

	if (localEnv == NULL) {
        printf("ERROR: failed to get ENV pointer in pushContext");
		//  S4JniExample_destroyJVM(jvm);

        return (JNIEnv*) NULL;
    }

	return localEnv;
}


/*!
 * \brief Create a new instance of the transcriber application
 * \return Pointer to the java object
 *
 * This method is used to create a new instance transcriber application
 */
jobject S4JniExample_createTranscriber(JavaVM *jvm){
	JNIEnv *env = getAttachedEnv(jvm);
	
	jclass cls;
	jmethodID mid;
	jobject transcriber;

	cls = (*env)->FindClass(env, TRANSCRIBER_CLASS);
    if (cls == NULL) {
        S4JniExample_destroyJVM(jvm);
    }

    jstring wavFileString = (*env)->NewStringUTF(env, "foobar.wav");

	mid = (*env)->GetMethodID(env, cls, "<init>", "()V");
	if (mid == NULL) {
        S4JniExample_destroyJVM(jvm);
    }

	transcriber = (*env)->NewObject(env, cls, mid, wavFileString);
	printf("instantiated Transcriber instance\n");
	
	//mid = (*env)->GetMethodID(env, cls, "main", "([Ljava/lang/String;)V");
	mid = (*env)->GetStaticMethodID(env, cls, "main", "([Ljava/lang/String;)V");
	
	if (mid == NULL) {
        S4JniExample_destroyJVM(jvm);
    }

	printf("calling main...\n");
	(*env)->CallStaticObjectMethod(env, cls, mid, NULL);
	printf("done\n");

	return transcriber;
}



int main(int argc, char **argv) {

	printf("create a jvm instance\n");

	JavaVM *jvm;
	jvm = S4JniExample_createJVM("lib");
	if (jvm == NULL) {
        fprintf(stderr, "Can't create Java VM\n");
       // exit(1);
    }

	printf("creating Transcriber... \n");

	jobject transcriber = S4JniExample_createTranscriber(jvm);

	return 0;
}
